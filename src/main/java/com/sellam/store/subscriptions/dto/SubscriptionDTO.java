package com.sellam.store.subscriptions.dto;

import com.sellam.store.subscriptions.models.SubscriptionPlanEnum;
import com.sellam.store.subscriptions.models.SubscriptionStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

public class SubscriptionDTO
{
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SubscriptionResponse
    {
        private UUID shopId;
        private SubscriptionPlanEnum plan;
        private SubscriptionStatusEnum status;
        private LocalDateTime trialEndsAt;
        private LocalDateTime currentPeriodEndsAt;
        private LocalDateTime graceUntil;
        private Integer bonusDaysEarned;

        /**
         * Nombre de jours restants avant blocage effectif, calculé côté
         * serveur pour éviter toute divergence de fuseau horaire côté
         * frontend. Négatif si déjà expiré (ne devrait pas arriver tant
         * que status != EXPIRED, mais gardé pour robustesse d'affichage).
         */
        private Long daysRemaining;

        /**
         * true si l'accès à l'application doit être bloqué (status == EXPIRED).
         * Le frontend s'appuie sur ce champ plutôt que de réinterpréter
         * status lui-même, pour centraliser la règle de blocage ici.
         */
        private boolean accessBlocked;

        /**
         * Message d'avertissement à afficher (façon banner Shopify) quand
         * l'essai approche de sa fin ou que le paiement est en retard.
         * Null si aucun avertissement à afficher.
         */
        private String warningMessage;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InitiateSubscriptionPaymentRequest
    {
        private UUID shopId;
    }
}
