package com.sellam.store.support.dto;

import com.sellam.store.support.models.TicketCategoryEnum;
import com.sellam.store.support.models.TicketPriorityEnum;
import com.sellam.store.support.models.TicketStatusEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class AddMessageRequest {
        @NotBlank(message = "Le message ne peut pas être vide")
        private String message;
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
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TicketDetailResponse {
        private TicketSummaryResponse ticket;
        private List<TicketMessageResponse> messages;
    }
}
