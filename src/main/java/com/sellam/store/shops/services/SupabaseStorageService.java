package com.sellam.store.shops.services;

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
import java.util.UUID;

/**
 * Upload de fichiers vers Supabase Storage via son API REST.
 * Pas de SDK Java officiel pour Supabase Storage : on appelle directement
 * POST /storage/v1/object/{bucket}/{path} avec la service_role key.
 */
@Service
@Slf4j
public class SupabaseStorageService
{
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/png", "image/jpeg", "image/webp"
    );
    private static final long MAX_FILE_SIZE_BYTES = 3 * 1024 * 1024; // 3 Mo

    @Value("${supabase.url}")
    private String supabaseUrl; // ex: https://xxxxx.supabase.co

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    private static final String SHOP_LOGOS_BUCKET = "shop-logos";
    private static final String PRODUCT_PICTURES_BUCKET = "product-pictures";
    private static final String PROFILE_PICTURES_BUCKET = "profile-pictures";

    private final RestTemplate restTemplate = new RestTemplate();

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

        String extension = extractExtension(file.getOriginalFilename());
        String objectPath = "shops/" + shopId + "/logo-" + UUID.randomUUID() + extension;

        byte[] bytes;
        try
        {
            bytes = file.getBytes();
        }
        catch (IOException e)
        {
            log.warn("Impossible de lire le fichier uploadé pour la boutique {} : {}", shopId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier illisible");
        }

        URI uploadUri = URI.create(supabaseUrl + "/storage/v1/object/" + SHOP_LOGOS_BUCKET + "/" + objectPath);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.set("apikey", serviceRoleKey);
        headers.setContentType(MediaType.parseMediaType(file.getContentType()));
        headers.set("x-upsert", "true");

        RequestEntity<byte[]> requestEntity = new RequestEntity<>(bytes, headers, HttpMethod.POST, uploadUri);

        ResponseEntity<String> response;
        try
        {
            response = restTemplate.exchange(requestEntity, String.class);
        }
        catch (RestClientException e)
        {
            log.error("Échec de l'upload vers Supabase Storage pour la boutique {} : {}", shopId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Échec de l'upload vers le stockage, réessayez");
        }

        if (!response.getStatusCode().is2xxSuccessful())
        {
            log.error("Supabase Storage a répondu {} pour la boutique {}", response.getStatusCode(), shopId);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Échec de l'upload vers le stockage, réessayez");
        }

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

        String extension = extractExtension(file.getOriginalFilename());
        String objectPath = "products/" + productId + "/picture-" + UUID.randomUUID() + extension;

        byte[] bytes;
        try
        {
            bytes = file.getBytes();
        }
        catch (IOException e)
        {
            log.warn("Impossible de lire le fichier uploadé pour le produit {} : {}", productId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier illisible");
        }

        URI uploadUri = URI.create(supabaseUrl + "/storage/v1/object/" + PRODUCT_PICTURES_BUCKET + "/" + objectPath);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.set("apikey", serviceRoleKey);
        headers.setContentType(MediaType.parseMediaType(file.getContentType()));
        headers.set("x-upsert", "true");

        RequestEntity<byte[]> requestEntity = new RequestEntity<>(bytes, headers, HttpMethod.POST, uploadUri);

        ResponseEntity<String> response;
        try
        {
            response = restTemplate.exchange(requestEntity, String.class);
        }
        catch (RestClientException e)
        {
            log.error("Échec de l'upload vers Supabase Storage pour le produit {} : {}", productId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Échec de l'upload vers le stockage, réessayez");
        }

        if (!response.getStatusCode().is2xxSuccessful())
        {
            log.error("Supabase Storage a répondu {} pour le produit {}", response.getStatusCode(), productId);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Échec de l'upload vers le stockage, réessayez");
        }

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

        String extension = extractExtension(file.getOriginalFilename());
        String objectPath = "users/" + userId + "/profile-picture-" + UUID.randomUUID() + extension;

        byte[] bytes;
        try
        {
            bytes = file.getBytes();
        }
        catch (IOException e)
        {
            log.warn("Impossible de lire le fichier uploadé pour le profil {} : {}", userId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier illisible");
        }

        URI uploadUri = URI.create(supabaseUrl + "/storage/v1/object/" + PROFILE_PICTURES_BUCKET + "/" + objectPath);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.set("apikey", serviceRoleKey);
        headers.setContentType(MediaType.parseMediaType(file.getContentType()));
        headers.set("x-upsert", "true");

        RequestEntity<byte[]> requestEntity = new RequestEntity<>(bytes, headers, HttpMethod.POST, uploadUri);

        ResponseEntity<String> response;
        try
        {
            response = restTemplate.exchange(requestEntity, String.class);
        }
        catch (RestClientException e)
        {
            log.error("Échec de l'upload vers Supabase Storage pour le profil {} : {}", userId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Échec de l'upload vers le stockage, réessayez");
        }

        if (!response.getStatusCode().is2xxSuccessful())
        {
            log.error("Supabase Storage a répondu {} pour le profil {}", response.getStatusCode(), userId);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Échec de l'upload vers le stockage, réessayez");
        }

        return supabaseUrl + "/storage/v1/object/public/" + PROFILE_PICTURES_BUCKET + "/" + objectPath;
    }

    private void validateFile(MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun fichier fourni");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES)
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier trop volumineux (max 3 Mo)");
        }
        if (file.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType()))
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format non supporté (PNG, JPEG ou WEBP uniquement)");
        }
    }

    private String extractExtension(String originalFilename)
    {
        if (originalFilename == null || !originalFilename.contains("."))
        {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}