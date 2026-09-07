package com.sellam.store.support.dto;

import com.sellam.store.support.models.TicketCategoryEnum;
import com.sellam.store.support.models.TicketPriorityEnum;
import com.sellam.store.support.models.TicketStatusEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

public class SupportDTO {

    // --- Requests ---

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CreateTicketRequest {
        @NotBlank(message = "Le sujet est requis")
        private String subject;

        @NotNull(message = "La catégorie est requise")
        private TicketCategoryEnum category;

        @NotNull(message = "La priorité est requise")
        private TicketPriorityEnum priority;

        @NotBlank(message = "Le message initial est requis")
        private String initialMessage;

        // URLs des pièces jointes déjà uploadées (via POST /support/attachments)
        // avant la création du ticket. Optionnel : la plupart des tickets n'en
        // ont pas.
        private List<String> attachmentUrls;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class AddMessageRequest {
        @NotBlank(message = "Le message ne peut pas être vide")
        private String message;

        // Mêmes URLs pré-uploadées, attachées à ce message de suivi.
        private List<String> attachmentUrls;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ResetIdentityLimitRequest {
        @NotBlank(message = "Un motif est obligatoire pour réinitialiser cette limite de sécurité")
        private String reason;
        
        private UUID ticketId; // Optionnel : lier l'action à un ticket existant
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UpdateTicketStatusRequest {
        @NotNull(message = "Le statut est requis")
        private TicketStatusEnum status;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class FindPersonRequest {
        @NotBlank(message = "Un email ou un numéro de téléphone est requis")
        private String identifier; // email ou téléphone, résolu côté service
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ManualSubscriptionActivationRequest {
        @NotBlank(message = "Un motif est obligatoire pour activer manuellement un abonnement")
        private String reason;

        @NotBlank(message = "La référence du paiement Mobile Money est requise")
        private String paymentReference; // ex: référence de transaction Orange Money / MTN MoMo

        @NotNull(message = "Le montant payé est requis")
        @Min(value = 1, message = "Le montant doit être positif")
        private Integer amount;

        private UUID ticketId; // optionnel, même logique que ResetIdentityLimitRequest
    }
    // --- Responses ---

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TicketSummaryResponse {
        private UUID id;
        private String authorName;
        private String subject;
        private TicketCategoryEnum category;
        private TicketStatusEnum status;
        private TicketPriorityEnum priority;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TicketMessageResponse {
        private UUID id;
        private UUID senderId;
        private String senderName;
        private String message;
        private boolean isSystemMessage;
        private LocalDateTime createdAt;
        private List<TicketAttachmentResponse> attachments;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TicketAttachmentResponse {
        private UUID id;
        private String fileUrl;
        private String fileName;
        private String contentType;
        private long fileSizeBytes;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TicketDetailResponse {
        private TicketSummaryResponse ticket;
        private List<TicketMessageResponse> messages;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class PersonWithShopsResponse {
        private UUID personId;
        private String name;
        private String email;
        private String phoneNumber;
        private List<ShopOption> shops;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ShopOption {
        private UUID shopId;
        private String shopName;
        private String subscriptionStatus; // affichage seulement, cf. SubscriptionStatusEnum
    }
}

