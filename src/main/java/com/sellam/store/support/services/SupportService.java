package com.sellam.store.support.services;

import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.support.dto.SupportDTO;
import com.sellam.store.support.models.*;
import com.sellam.store.support.repositories.SupportAuditLogRepository;
import com.sellam.store.support.repositories.TicketAttachmentRepository;
import com.sellam.store.support.repositories.TicketMessageRepository;
import com.sellam.store.support.repositories.TicketRepository;
import com.sellam.store.identity.models.ShopMembershipEntity;
import com.sellam.store.identity.repositories.ShopMembershipRepository;
import com.sellam.store.payments.models.PaymentTransactionEntity;
import com.sellam.store.payments.models.PaymentTransactionStatusEnum;
import com.sellam.store.payments.repositories.PaymentTransactionRepository;
import com.sellam.store.subscriptions.models.SubscriptionEntity;
import com.sellam.store.subscriptions.repositories.SubscriptionRepository;
import com.sellam.store.subscriptions.services.SubscriptionService;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.services.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportService {

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final SupportAuditLogRepository auditLogRepository;
    private final PersonRepository personRepository;
    private final ShopMembershipRepository shopMembershipRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SupabaseStorageService supabaseStorageService;


    // --- User Actions ---

    @Transactional
    public SupportDTO.TicketSummaryResponse createTicket(UUID authorId, SupportDTO.CreateTicketRequest request) {
        PersonEntity author = findPerson(authorId);

        TicketEntity ticket = TicketEntity.builder()
                .author(author)
                .subject(request.getSubject())
                .category(request.getCategory())
                .priority(request.getPriority())
                .status(TicketStatusEnum.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        ticket = ticketRepository.save(ticket);

        TicketMessageEntity message = TicketMessageEntity.builder()
                .ticket(ticket)
                .sender(author)
                .message(request.getInitialMessage())
                .isSystemMessage(false)
                .createdAt(LocalDateTime.now())
                .build();
        message = ticketMessageRepository.save(message);

        attachAttachments(message, request.getAttachmentUrls());

        return toTicketSummary(ticket);
    }

    @Transactional
    public SupportDTO.TicketMessageResponse addMessage(UUID ticketId, UUID senderId, SupportDTO.AddMessageRequest request, boolean isAdmin) {
        TicketEntity ticket = findTicket(ticketId);
        PersonEntity sender = findPerson(senderId);

        // Update status automatically
        if (isAdmin) {
            ticket.setStatus(TicketStatusEnum.WAITING_FOR_USER);
        } else {
            ticket.setStatus(TicketStatusEnum.IN_PROGRESS);
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        TicketMessageEntity message = TicketMessageEntity.builder()
                .ticket(ticket)
                .sender(sender)
                .message(request.getMessage())
                .isSystemMessage(false)
                .createdAt(LocalDateTime.now())
                .build();
        message = ticketMessageRepository.save(message);

        attachAttachments(message, request.getAttachmentUrls());

        return toMessageResponse(message);
    }

    public List<SupportDTO.TicketSummaryResponse> listUserTickets(UUID authorId) {
        return ticketRepository.findByAuthorIdOrderByCreatedAtDesc(authorId)
                .stream().map(this::toTicketSummary).collect(Collectors.toList());
    }

    // --- Admin Actions ---

    public List<SupportDTO.TicketSummaryResponse> listAllTickets() {
        return ticketRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toTicketSummary).collect(Collectors.toList());
    }

    @Transactional
    public SupportDTO.TicketSummaryResponse updateTicketStatus(UUID ticketId, SupportDTO.UpdateTicketStatusRequest request) {
        TicketEntity ticket = findTicket(ticketId);
        ticket.setStatus(request.getStatus());
        ticket.setUpdatedAt(LocalDateTime.now());
        
        if (request.getStatus() == TicketStatusEnum.RESOLVED || request.getStatus() == TicketStatusEnum.CLOSED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        
        return toTicketSummary(ticketRepository.save(ticket));
    }

    @Transactional
    public void resetIdentityChangeLimit(UUID adminId, UUID targetUserId, SupportDTO.ResetIdentityLimitRequest request) {
        PersonEntity admin = findPerson(adminId);
        PersonEntity targetUser = findPerson(targetUserId);

        // 1. Reset the limit
        targetUser.setLastEmailChangeAt(null);
        targetUser.setLastPhoneChangeAt(null);
        personRepository.save(targetUser);

        // 2. Audit Log
        TicketEntity relatedTicket = null;
        if (request.getTicketId() != null) {
            relatedTicket = findTicket(request.getTicketId());
        }

        SupportAuditLogEntity auditLog = SupportAuditLogEntity.builder()
                .admin(admin)
                .targetUser(targetUser)
                .actionType(SupportActionTypeEnum.RESET_IDENTITY_CHANGE_LIMIT)
                .reason(request.getReason())
                .ticket(relatedTicket)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        log.info("L'administrateur {} a réinitialisé la limite d'identité pour {}. Motif : {}", 
                admin.getName(), targetUser.getName(), request.getReason());

        // 3. System message in ticket if provided
        if (relatedTicket != null) {
            TicketMessageEntity systemMessage = TicketMessageEntity.builder()
                    .ticket(relatedTicket)
                    .sender(admin) // The admin triggered it
                    .message("Action Système : L'administrateur a réinitialisé la limite de changement de téléphone/email.")
                    .isSystemMessage(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            ticketMessageRepository.save(systemMessage);
        }
    }

    public SupportDTO.PersonWithShopsResponse findPersonWithShops(String identifier) {
        PersonEntity person = personRepository.findByEmail(identifier)
                .or(() -> personRepository.findByPhoneNumber(identifier))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun utilisateur trouvé avec cet email ou ce numéro de téléphone"));

        List<ShopMembershipEntity> memberships = shopMembershipRepository.findByPersonId(person.getId());

        List<SupportDTO.ShopOption> shops = memberships.stream()
                .filter(ShopMembershipEntity::isActive)
                .map(m -> {
                    ShopEntity shop = m.getShop();
                    String status = subscriptionRepository.findByShopId(shop.getId())
                            .map(s -> s.getStatus().name())
                            .orElse("AUCUN_ABONNEMENT");
                    return SupportDTO.ShopOption.builder()
                            .shopId(shop.getId())
                            .shopName(shop.getName())
                            .subscriptionStatus(status)
                            .build();
                })
                .collect(Collectors.toList());

        return SupportDTO.PersonWithShopsResponse.builder()
                .personId(person.getId())
                .name(person.getName())
                .email(person.getEmail())
                .phoneNumber(person.getPhoneNumber())
                .shops(shops)
                .build();
    }

    /**
     * Active manuellement l'abonnement d'une boutique suite à un paiement
     * Mobile Money reçu hors CinetPay (en attendant l'activation complète
     * du compte marchand CinetPay). Réutilise SubscriptionService.confirmPayment
     * pour garantir un comportement strictement identique à un paiement
     * CinetPay réel (mêmes effets : passage en ACTIVE, prolongation de la
     * période, déclenchement de la récompense de parrainage si premier
     * paiement) — la seule différence est l'origine de la confirmation.
     *
     * Crée aussi une PaymentTransactionEntity ACCEPTED pour garder un
     * historique financier complet même hors CinetPay, et un log d'audit
     * immuable (même pattern que resetIdentityChangeLimit).
     */
    @Transactional
    public void activateSubscriptionManually(
            UUID adminId, UUID shopId, SupportDTO.ManualSubscriptionActivationRequest request) {

        PersonEntity admin = findPerson(adminId);

        SubscriptionEntity subscription = subscriptionRepository.findByShopId(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun abonnement trouvé pour cette boutique"));
        ShopEntity shop = subscription.getShop();

        // Un gérant/employé de la boutique sert de "targetUser" pour l'audit
        // log (qui cible une personne, pas une boutique) : le premier membre
        // actif suffit, il ne s'agit que de traçabilité, pas d'une donnée
        // métier utilisée pour l'activation elle-même.
        PersonEntity targetUser = shopMembershipRepository.findByShopId(shopId).stream()
                .filter(ShopMembershipEntity::isActive)
                .map(ShopMembershipEntity::getPerson)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Aucun membre actif trouvé pour cette boutique"));

        String transactionId = "MANUAL-" + shopId + "-" + System.currentTimeMillis();
        PaymentTransactionEntity transaction = PaymentTransactionEntity.builder()
                .transactionId(transactionId)
                .shop(shop)
                .amount(request.getAmount())
                .currency("XOF")
                .status(PaymentTransactionStatusEnum.ACCEPTED)
                .manualPaymentReference(request.getPaymentReference())
                .manualNote("Paiement manuel confirmé par l'administrateur " + admin.getName()
                        + ". Motif : " + request.getReason())
                .confirmedAt(LocalDateTime.now())
                .build();
        paymentTransactionRepository.save(transaction);

        subscriptionService.confirmPayment(shopId);

        TicketEntity relatedTicket = null;
        if (request.getTicketId() != null) {
            relatedTicket = findTicket(request.getTicketId());
        }

        SupportAuditLogEntity auditLog = SupportAuditLogEntity.builder()
                .admin(admin)
                .targetUser(targetUser)
                .actionType(SupportActionTypeEnum.MANUAL_SUBSCRIPTION_ACTIVATION)
                .reason(request.getReason() + " (référence paiement : " + request.getPaymentReference()
                        + ", montant : " + request.getAmount() + " XOF, transaction : " + transactionId + ")")
                .ticket(relatedTicket)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        log.info("L'administrateur {} a activé manuellement l'abonnement de la boutique {} (transaction {}). Motif : {}",
                admin.getName(), shop.getName(), transactionId, request.getReason());
    }

    // --- Queries ---

    public SupportDTO.TicketDetailResponse getTicketDetail(UUID ticketId, UUID userId, boolean isAdmin) {
        TicketEntity ticket = findTicket(ticketId);
        
        // Security check: only author or admin can view
        if (!isAdmin && !ticket.getAuthor().getId().equals(userId)) {
            throw new IllegalStateException("Vous n'êtes pas autorisé à voir ce ticket");
        }

        List<SupportDTO.TicketMessageResponse> messages = ticketMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream().map(this::toMessageResponse).collect(Collectors.toList());

        return SupportDTO.TicketDetailResponse.builder()
                .ticket(toTicketSummary(ticket))
                .messages(messages)
                .build();
    }

    // --- Helpers ---

    /**
     * Persiste les pièces jointes (déjà uploadées vers Supabase Storage via
     * POST /support/attachments) en les rattachant au message donné.
     * fileName/contentType/fileSizeBytes ne sont pas connus côté serveur à
     * ce stade (seule l'URL est transmise) : on les dérive au mieux depuis
     * l'URL elle-même, ce qui suffit pour l'affichage (nom de fichier dans
     * la conversation, icône selon l'extension).
     */
    private void attachAttachments(TicketMessageEntity message, List<String> attachmentUrls) {
        if (attachmentUrls == null || attachmentUrls.isEmpty()) {
            return;
        }
        for (String url : attachmentUrls) {
            if (url == null || url.isBlank()) {
                continue;
            }
            String fileName = url.substring(url.lastIndexOf('/') + 1);
            String extension = fileName.contains(".")
                    ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase()
                    : "";
            String contentType = switch (extension) {
                case "png" -> "image/png";
                case "webp" -> "image/webp";
                case "jpg", "jpeg" -> "image/jpeg";
                default -> "application/octet-stream";
            };

            TicketAttachmentEntity attachment = TicketAttachmentEntity.builder()
                    .message(message)
                    .fileUrl(url)
                    .fileName(fileName)
                    .contentType(contentType)
                    .fileSizeBytes(0L) // taille non re-transmise après upload ; non critique pour l'affichage
                    .createdAt(LocalDateTime.now())
                    .build();
            ticketAttachmentRepository.save(attachment);
        }
    }

    private PersonEntity findPerson(UUID id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    private TicketEntity findTicket(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable"));
    }

    private SupportDTO.TicketSummaryResponse toTicketSummary(TicketEntity ticket) {
        return SupportDTO.TicketSummaryResponse.builder()
                .id(ticket.getId())
                .authorName(ticket.getAuthor().getName())
                .subject(ticket.getSubject())
                .category(ticket.getCategory())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    private SupportDTO.TicketMessageResponse toMessageResponse(TicketMessageEntity message) {
        List<SupportDTO.TicketAttachmentResponse> attachments = ticketAttachmentRepository
                .findByMessageId(message.getId())
                .stream()
                .map(a -> SupportDTO.TicketAttachmentResponse.builder()
                        .id(a.getId())
                        // Signature à la lecture : le champ fileUrl stocké en
                        // base est un objectPath relatif (bucket privé), on
                        // le transforme en URL signée temporaire (1h) juste
                        // avant de le renvoyer au frontend. Si la signature
                        // échoue pour une pièce jointe isolée (fichier
                        // supprimé du bucket, erreur réseau ponctuelle), on
                        // ne fait pas échouer tout l'affichage du ticket :
                        // on journalise et on renvoie null pour cette seule
                        // pièce jointe.
                        .fileUrl(signAttachmentUrlSafely(a.getFileUrl()))
                        .fileName(a.getFileName())
                        .contentType(a.getContentType())
                        .fileSizeBytes(a.getFileSizeBytes())
                        .build())
                .collect(Collectors.toList());
 

        return SupportDTO.TicketMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSender() != null ? message.getSender().getId() : null)
                .senderName(message.getSender() != null ? message.getSender().getName() : "Système")
                .message(message.getMessage())
                .isSystemMessage(message.isSystemMessage())
                .createdAt(message.getCreatedAt())
                .attachments(attachments)
                .build();
    }


    private String signAttachmentUrlSafely(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) {
            return null;
        }
        try {
            return supabaseStorageService.generateSignedTicketAttachmentUrl(objectPath);
        } catch (Exception e) {
            log.warn("Impossible de signer l'URL de la pièce jointe {} : {}", objectPath, e.getMessage());
            return null;
        }
    }
    
}

