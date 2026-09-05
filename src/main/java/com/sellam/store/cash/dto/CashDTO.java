package com.sellam.store.cash.dto;

import com.sellam.store.cash.models.CashMovementTypeEnum;
import com.sellam.store.cash.models.CashSessionStatusEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class CashDTO {

    // ────────────────── Register ──────────────────

    @Data @AllArgsConstructor @NoArgsConstructor @Builder
    public static class RegisterResponse {
        private UUID id;
        private UUID shopId;
        private String label;
        private boolean active;
        private SessionSummary activeSession;
    }

    @Data @AllArgsConstructor @NoArgsConstructor @Builder
    public static class CreateRegisterRequest {
        @NotBlank(message = "Le libellé de la caisse est requis")
        private String label;
    }

    // ────────────────── Session ──────────────────

    @Data @AllArgsConstructor @NoArgsConstructor @Builder
    public static class SessionSummary {
        private UUID id;
        private UUID registerId;
        private String registerLabel;
        private CashSessionStatusEnum status;
        private String openedByName;
        private LocalDateTime openedAt;
        private BigDecimal openingCashAmount;
        private String closedByName;
        private LocalDateTime closedAt;
        private BigDecimal closingDeclaredAmount;
        private BigDecimal closingComputedAmount;
        private BigDecimal discrepancy;
    }

    @Data @AllArgsConstructor @NoArgsConstructor @Builder
    public static class OpenSessionRequest {
        @NotNull(message = "Le fonds de caisse initial est requis")
        @DecimalMin(value = "0.0", message = "Le fonds de caisse ne peut pas être négatif")
        private BigDecimal openingCashAmount;
    }

    @Data @AllArgsConstructor @NoArgsConstructor @Builder
    public static class CloseSessionRequest {
        @NotNull(message = "Le montant déclaré est requis")
        @DecimalMin(value = "0.0", message = "Le montant déclaré ne peut pas être négatif")
        private BigDecimal closingDeclaredAmount;
    }

    @Data @AllArgsConstructor @NoArgsConstructor @Builder
    public static class RegularizeInitialCashRequest {
        @NotNull(message = "Le fonds de caisse initial réel est requis")
        @DecimalMin(value = "0.0", message = "Le fonds de caisse ne peut pas être négatif")
        private BigDecimal actualOpeningCashAmount;
    }

    @Data @AllArgsConstructor @NoArgsConstructor @Builder
    public static class HandoverCountRequest {
        @NotNull(message = "Le montant compté est requis")
        @DecimalMin(value = "0.0", message = "Le montant compté ne peut pas être négatif")
        private BigDecimal countedAmount;

        @NotNull(message = "L'instant du comptage est requis")
        private LocalDateTime countedAt;
    }

    // ────────────────── Movement ──────────────────

    @Data @AllArgsConstructor @NoArgsConstructor @Builder
    public static class MovementResponse {
        private UUID id;
        private UUID sessionId;
        private CashMovementTypeEnum type;
        private BigDecimal amount;
        private String reason;
        private UUID referenceInvoiceId;
        private String effectueParName;
        private LocalDateTime timestamp;
    }

    @Data @AllArgsConstructor @NoArgsConstructor @Builder
    public static class CreateMovementRequest {
        @NotNull(message = "Le type de mouvement est requis")
        private CashMovementTypeEnum type;

        @NotNull(message = "Le montant est requis")
        @DecimalMin(value = "0.01", message = "Le montant doit être positif")
        private BigDecimal amount;

        private String reason;

        private UUID referenceInvoiceId;
    }

    // ────────────────── Session Detail ──────────────────

    @Data @AllArgsConstructor @NoArgsConstructor @Builder
    public static class SessionDetailResponse {
        private SessionSummary session;
        private List<MovementResponse> movements;
        private BigDecimal currentBalance;
    }

    // ────────────────── Dashboard ──────────────────

    @Data @AllArgsConstructor @NoArgsConstructor @Builder
    public static class CashStatusResponse {
        private boolean hasActiveSession;
        private boolean pendingInitialCash;
        private boolean hasPendingHandover;
        private SessionSummary activeSession;
        private UUID pendingHandoverSessionId;
        private String pendingHandoverPreviousUserName;
    }
}
