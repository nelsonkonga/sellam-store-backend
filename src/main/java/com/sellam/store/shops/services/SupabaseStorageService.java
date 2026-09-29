package com.sellam.store.shops.services;

import com.sellam.store.common.images.ImageCompressionService;
import com.sellam.store.common.images.ImageCompressionService.PreparedImage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Upload de fichiers vers Supabase Storage via son API REST.
 * Pas de SDK Java officiel pour Supabase Storage : on appelle directement
 * POST /storage/v1/object/{bucket}/{path} avec la service_role key.
 *
 * shop-logos, product-pictures et profile-pictures restent des buckets
 * PUBLICS côté Supabase (contenu déjà destiné à être vu publiquement dans
 * l'app) : leurs méthodes d'upload continuent de renvoyer directement
 * l'URL publique finale.
 *
 * ticket-attachments doit rester un bucket PRIVÉ côté Supabase (dashboard
 * Supabase > Storage > ticket-attachments > Settings > désactiver "Public
 * bucket") car ces images peuvent contenir des informations sensibles
 * (preuves de paiement Mobile Money, captures d'écran de compte). Pour ce
 * bucket, uploadTicketAttachment() renvoie désormais un objectPath (chemin
 * relatif dans le bucket, pas une URL), à stocker tel quel en base.
 * L'URL signée temporaire n'est générée qu'à la demande, au moment de
 * l'affichage, via generateSignedTicketAttachmentUrl().
 */
@Service
@Slf4j
public class SupabaseStorageService
{
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/png", "image/jpeg", "image/webp"
    );
    private static final long MAX_FILE_SIZE_BYTES = 3 * 1024 * 1024; // 3 Mo
    private static final long TICKET_ATTACHMENT_MAX_SIZE_BYTES = 2 * 1024 * 1024; // 2 Mo

    // Durée de validité des URLs signées pour les pièces jointes de ticket.
    // 1h suffit pour l'ouverture d'un ticket ou d'une conversation dans
    // l'app ; l'URL est régénérée à chaque nouvelle requête de lecture,
    // donc pas besoin d'une durée plus longue.
    private static final int TICKET_ATTACHMENT_SIGNED_URL_TTL_SECONDS = 3600;

    @Value("${supabase.url}")
    private String supabaseUrl; // ex: https://xxxxx.supabase.co

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    private static final String SHOP_LOGOS_BUCKET = "shop-logos";
    private static final String PRODUCT_PICTURES_BUCKET = "product-pictures";
    private static final String PROFILE_PICTURES_BUCKET = "profile-pictures";
    private static final String TICKET_ATTACHMENTS_BUCKET = "ticket-attachments";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ImageCompressionService imageCompressionService;

    public SupabaseStorageService(ImageCompressionService imageCompressionService)
    {
        this.imageCompressionService = imageCompressionService;
    }

    /**
     * Upload un logo de boutique et retourne son URL publique.
     * Le nom de fichier est régénéré (UUID) pour éviter collisions et
     * problèmes d'encodage ; l'extension d'origine est conservée.
     */
    public String uploadShopLogo(UUID shopId, MultipartFile file)
    {
        if (supabaseUrl == null || supabaseUrl.isBlank() || serviceRoleKey == null || serviceRoleKey.isBlank())
        {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Le stockage de logos n'est pas configuré");
        }

        validateFile(file);

        PreparedImage image = prepare(file, ImageCompressionService.Use.LOGO, "la boutique " + shopId);
        String objectPath = "shops/" + shopId + "/logo-" + UUID.randomUUID() + image.extension();

        uploadToBucket(SHOP_LOGOS_BUCKET, objectPath, image.bytes(), image.contentType(), "la boutique " + shopId);

        return supabaseUrl + "/storage/v1/object/public/" + SHOP_LOGOS_BUCKET + "/" + objectPath;
    }

    /**
     * Upload une photo de produit et retourne son URL publique.
     */
    public String uploadProductPicture(UUID productId, MultipartFile file)
    {
        if (supabaseUrl == null || supabaseUrl.isBlank() || serviceRoleKey == null || serviceRoleKey.isBlank())
        {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Le stockage de photos n'est pas configuré");
        }

        validateFile(file);

        PreparedImage image = prepare(file, ImageCompressionService.Use.PRODUCT, "le produit " + productId);
        String objectPath = "products/" + productId + "/picture-" + UUID.randomUUID() + image.extension();

        uploadToBucket(PRODUCT_PICTURES_BUCKET, objectPath, image.bytes(), image.contentType(), "le produit " + productId);

        return supabaseUrl + "/storage/v1/object/public/" + PRODUCT_PICTURES_BUCKET + "/" + objectPath;
    }

    /**
     * Upload une photo de profil et retourne son URL publique.
     */
    public String uploadProfilePicture(UUID userId, MultipartFile file)
    {
        if (supabaseUrl == null || supabaseUrl.isBlank() || serviceRoleKey == null || serviceRoleKey.isBlank())
        {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Le stockage de photos n'est pas configuré");
        }

        validateFile(file);

        PreparedImage image = prepare(file, ImageCompressionService.Use.PROFILE, "le profil " + userId);
        String objectPath = "users/" + userId + "/profile-picture-" + UUID.randomUUID() + image.extension();

        uploadToBucket(PROFILE_PICTURES_BUCKET, objectPath, image.bytes(), image.contentType(), "le profil " + userId);

        return supabaseUrl + "/storage/v1/object/public/" + PROFILE_PICTURES_BUCKET + "/" + objectPath;
    }

    /**
     * Upload une pièce jointe (image) pour un ticket de support et retourne
     * son objectPath (chemin RELATIF dans le bucket privé ticket-attachments,
     * PAS une URL). C'est ce objectPath qu'il faut stocker tel quel en base
     * (attachmentUrls / fileUrl) : il n'est pas utilisable directement dans
     * un <img src>, il doit passer par generateSignedTicketAttachmentUrl()
     * au moment de l'affichage.
     * Limite de taille dédiée, plus stricte que les autres uploads (2 Mo) :
     * les captures d'écran de preuve de paiement ou de bug n'ont pas besoin
     * d'être volumineuses, et ça évite de saturer le bucket avec des
     * fichiers envoyés depuis un mobile en pleine résolution.
     */
    public String uploadTicketAttachment(UUID ticketId, MultipartFile file)
    {
        if (supabaseUrl == null || supabaseUrl.isBlank() || serviceRoleKey == null || serviceRoleKey.isBlank())
        {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Le stockage de pièces jointes n'est pas configuré");
        }

        validateFile(file, TICKET_ATTACHMENT_MAX_SIZE_BYTES);

        PreparedImage image = prepare(file, ImageCompressionService.Use.ATTACHMENT, "le ticket " + ticketId);
        String objectPath = "tickets/" + ticketId + "/" + UUID.randomUUID() + image.extension();

        uploadToBucket(TICKET_ATTACHMENTS_BUCKET, objectPath, image.bytes(), image.contentType(), "le ticket " + ticketId);

        return objectPath;
    }

    /**
     * Génère une URL signée temporaire (valable 1h) pour lire une pièce
     * jointe de ticket depuis le bucket privé ticket-attachments.
     * objectPath doit être exactement la valeur renvoyée précédemment par
     * uploadTicketAttachment() (et donc déjà stockée en base) — pas une URL
     * complète.
     * À appeler à chaque lecture (affichage d'un ticket) : ne jamais
     * stocker l'URL signée elle-même, elle expire.
     */
    public String generateSignedTicketAttachmentUrl(String objectPath)
    {
        if (supabaseUrl == null || supabaseUrl.isBlank() || serviceRoleKey == null || serviceRoleKey.isBlank())
        {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Le stockage de pièces jointes n'est pas configuré");
        }

        // Filet de sécurité : si une ancienne entrée en base contient encore
        // une URL publique complète (données créées avant ce changement),
        // on en extrait le chemin relatif plutôt que d'échouer.
        String normalizedPath = extractObjectPathIfFullUrl(objectPath);

        URI signUri = URI.create(supabaseUrl + "/storage/v1/object/sign/" + TICKET_ATTACHMENTS_BUCKET + "/" + normalizedPath);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.set("apikey", serviceRoleKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"expiresIn\": " + TICKET_ATTACHMENT_SIGNED_URL_TTL_SECONDS + "}";
        RequestEntity<String> requestEntity = new RequestEntity<>(body, headers, HttpMethod.POST, signUri);

        ResponseEntity<Map> response;
        try
        {
            response = restTemplate.exchange(requestEntity, Map.class);
        }
        catch (RestClientException e)
        {
            log.error("Échec de la génération d'URL signée pour la pièce jointe {} : {}", normalizedPath, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Impossible de charger la pièce jointe, réessayez");
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null
                || response.getBody().get("signedURL") == null)
        {
            log.error("Supabase Storage a répondu {} pour la signature de {}", response.getStatusCode(), normalizedPath);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Impossible de charger la pièce jointe, réessayez");
        }

        String signedPath = (String) response.getBody().get("signedURL");
        // signedPath ressemble à "/object/sign/ticket-attachments/tickets/.../xxx.jpg?token=..."
        return supabaseUrl + "/storage/v1" + signedPath;
    }

    private String extractObjectPathIfFullUrl(String value)
    {
        String marker = "/object/public/" + TICKET_ATTACHMENTS_BUCKET + "/";
        int index = value.indexOf(marker);
        if (index == -1)
        {
            return value; // déjà un objectPath relatif
        }
        return value.substring(index + marker.length());
    }

    private void uploadToBucket(String bucket, String objectPath, byte[] bytes, String contentType, String contextLabel)
    {
        URI uploadUri = URI.create(supabaseUrl + "/storage/v1/object/" + bucket + "/" + objectPath);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.set("apikey", serviceRoleKey);
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.set("x-upsert", "true");

        RequestEntity<byte[]> requestEntity = new RequestEntity<>(bytes, headers, HttpMethod.POST, uploadUri);

        ResponseEntity<String> response;
        try
        {
            response = restTemplate.exchange(requestEntity, String.class);
        }
        catch (RestClientException e)
        {
            log.error("Échec de l'upload vers Supabase Storage pour {} : {}", contextLabel, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Échec de l'upload vers le stockage, réessayez");
        }

        if (!response.getStatusCode().is2xxSuccessful())
        {
            log.error("Supabase Storage a répondu {} pour {}", response.getStatusCode(), contextLabel);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Échec de l'upload vers le stockage, réessayez");
        }
    }

    private PreparedImage prepare(MultipartFile file, ImageCompressionService.Use use, String contextLabel)
    {
        return imageCompressionService.prepare(readBytes(file, contextLabel), file.getContentType(), use);
    }

    private byte[] readBytes(MultipartFile file, String contextLabel)
    {
        try
        {
            return file.getBytes();
        }
        catch (IOException e)
        {
            log.warn("Impossible de lire le fichier uploadé pour {} : {}", contextLabel, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier illisible");
        }
    }

    private void validateFile(MultipartFile file)
    {
        validateFile(file, MAX_FILE_SIZE_BYTES);
    }

    private void validateFile(MultipartFile file, long maxSizeBytes)
    {
        if (file == null || file.isEmpty())
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun fichier fourni");
        }
        if (file.getSize() > maxSizeBytes)
        {
            long maxSizeMb = maxSizeBytes / (1024 * 1024);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier trop volumineux (max " + maxSizeMb + " Mo)");
        }
        if (file.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType()))
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format non supporté (PNG, JPEG ou WEBP uniquement)");
        }
    }
}