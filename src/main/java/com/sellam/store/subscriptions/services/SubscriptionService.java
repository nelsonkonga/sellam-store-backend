package com.sellam.store.subscriptions.services;

import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import com.sellam.store.subscriptions.dto.SubscriptionDTO;
import com.sellam.store.subscriptions.models.SubscriptionEntity;
import com.sellam.store.subscriptions.models.SubscriptionPlanEnum;
import com.sellam.store.subscriptions.models.SubscriptionStatusEnum;
import com.sellam.store.subscriptions.repositories.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Centralise toute la logique de statut d'abonnement.
 *
 * Règle volontairement simple (un seul plan payant, cf. discussion produit) :
 *
 *   TRIAL --(J-warningDays)--> TRIAL_ENDING --(trialEndsAt dépassé,
 *   pas de paiement confirmé)--> EXPIRED
 *
 *   ACTIVE --(currentPeriodEndsAt dépassé)--> PAST_DUE --(graceUntil dépassé)--> EXPIRED
 *
 * Le recalcul est fait à la lecture (getOrRecalculate) ET par un scheduler
 * périodique (SubscriptionScheduler) qui s'assure que le statut est à jour
 * même si personne n'a consulté la boutique entre-temps (utile pour bloquer
 * l'accès dès l'expiration, pas seulement au prochain login).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService
{
    private final SubscriptionRepository subscriptionRepository;
    private final ShopRepository shopRepository;

    @Value("${app.subscription.trial-days:14}")
    private int trialDays;

    @Value("${app.subscription.warning-before-days:3}")
    private int warningBeforeDays;

    @Value("${app.subscription.grace-days:3}")
    private int graceDays;

    @Value("${app.subscription.plan-duration-days:30}")
    private int planDurationDays;

    /**
     * Crée l'abonnement d'essai pour une boutique fraîchement créée.
     * Appelé depuis ShopService.createShop (à brancher).
     */
    @Transactional
    public SubscriptionEntity createTrialSubscription(ShopEntity shop)
    {
        SubscriptionEntity subscription = SubscriptionEntity.builder()
                .shop(shop)
                .plan(SubscriptionPlanEnum.FREE_TRIAL)
                .status(SubscriptionStatusEnum.TRIAL)
                .trialEndsAt(LocalDateTime.now().plusDays(trialDays))
                .bonusDaysEarned(0)
                .build();
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public SubscriptionEntity getOrRecalculate(UUID shopId)
    {
        SubscriptionEntity subscription = subscriptionRepository.findByShopId(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun abonnement trouvé pour cette boutique"));
        recalculate(subscription);
        return subscriptionRepository.save(subscription);
    }

    /**
     * Recalcule le statut en place, sans persister (laissé à l'appelant).
     * Séparé de getOrRecalculate() pour être réutilisable depuis le
     * scheduler qui traite des lots d'entités déjà chargées.
     */
    public void recalculate(SubscriptionEntity subscription)
    {
        LocalDateTime now = LocalDateTime.now();
        SubscriptionStatusEnum status = subscription.getStatus();

        if (status == SubscriptionStatusEnum.TRIAL || status == SubscriptionStatusEnum.TRIAL_ENDING)
        {
            LocalDateTime trialEndsAt = subscription.getTrialEndsAt();
            if (trialEndsAt == null)
            {
                return;
            }
            if (now.isAfter(trialEndsAt))
            {
                subscription.setStatus(SubscriptionStatusEnum.EXPIRED);
            }
            else if (now.isAfter(trialEndsAt.minusDays(warningBeforeDays)))
            {
                subscription.setStatus(SubscriptionStatusEnum.TRIAL_ENDING);
            }
            return;
        }

        if (status == SubscriptionStatusEnum.ACTIVE)
        {
            LocalDateTime periodEnd = subscription.getCurrentPeriodEndsAt();
            if (periodEnd != null && now.isAfter(periodEnd))
            {
                subscription.setStatus(SubscriptionStatusEnum.PAST_DUE);
                subscription.setGraceUntil(periodEnd.plusDays(graceDays));
            }
            return;
        }

        if (status == SubscriptionStatusEnum.PAST_DUE)
        {
            LocalDateTime graceUntil = subscription.getGraceUntil();
            if (graceUntil != null && now.isAfter(graceUntil))
            {
                subscription.setStatus(SubscriptionStatusEnum.EXPIRED);
            }
        }
        // EXPIRED est terminal tant qu'aucun paiement n'est confirmé
        // (cf. confirmPayment, qui réactive explicitement).
    }

    /**
     * Appelé par le webhook CinetPay lorsqu'un paiement est confirmé.
     * Prolonge (ou démarre) la période payée et repasse le statut à ACTIVE,
     * quel que soit l'état précédent (TRIAL, TRIAL_ENDING, PAST_DUE, EXPIRED).
     *
     * Retourne true si c'est le tout premier paiement confirmé pour cette
     * boutique — signal utilisé par le module de parrainage pour savoir
     * s'il doit déclencher la récompense du parrain.
     */
    @Transactional
    public boolean confirmPayment(UUID shopId)
    {
        SubscriptionEntity subscription = subscriptionRepository.findByShopId(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun abonnement trouvé pour cette boutique"));

        boolean isFirstPayment = subscription.getFirstPaymentConfirmedAt() == null;

        LocalDateTime now = LocalDateTime.now();
        // Si la période en cours n'est pas encore expirée, on prolonge à
        // partir de sa fin (renouvellement anticipé) plutôt que de perdre
        // les jours restants ; sinon on repart de maintenant.
        LocalDateTime base = (subscription.getCurrentPeriodEndsAt() != null
                && subscription.getCurrentPeriodEndsAt().isAfter(now))
                ? subscription.getCurrentPeriodEndsAt()
                : now;

        subscription.setPlan(SubscriptionPlanEnum.STANDARD);
        subscription.setStatus(SubscriptionStatusEnum.ACTIVE);
        subscription.setCurrentPeriodEndsAt(base.plusDays(planDurationDays));
        subscription.setGraceUntil(null);

        if (isFirstPayment)
        {
            subscription.setFirstPaymentConfirmedAt(now);
        }

        subscriptionRepository.save(subscription);
        return isFirstPayment;
    }

    /**
     * Ajoute des jours bonus à l'abonnement (récompense de parrainage,
     * geste commercial...). S'applique à trialEndsAt si l'essai est en
     * cours, sinon à currentPeriodEndsAt (ou démarre une période si aucune
     * n'existe encore, ce qui active immédiatement la boutique).
     */
    @Transactional
    public void grantBonusDays(UUID shopId, int days)
    {
        SubscriptionEntity subscription = subscriptionRepository.findByShopId(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun abonnement trouvé pour cette boutique"));

        LocalDateTime now = LocalDateTime.now();

        if (subscription.getStatus() == SubscriptionStatusEnum.TRIAL
                || subscription.getStatus() == SubscriptionStatusEnum.TRIAL_ENDING)
        {
            LocalDateTime baseTrial = subscription.getTrialEndsAt() != null && subscription.getTrialEndsAt().isAfter(now)
                    ? subscription.getTrialEndsAt()
                    : now;
            subscription.setTrialEndsAt(baseTrial.plusDays(days));
            // Redevient TRIAL si l'ajout de jours l'éloigne assez de l'échéance.
            recalculate(subscription);
        }
        else
        {
            LocalDateTime basePeriod = subscription.getCurrentPeriodEndsAt() != null
                    && subscription.getCurrentPeriodEndsAt().isAfter(now)
                    ? subscription.getCurrentPeriodEndsAt()
                    : now;
            subscription.setCurrentPeriodEndsAt(basePeriod.plusDays(days));
            subscription.setStatus(SubscriptionStatusEnum.ACTIVE);
            subscription.setGraceUntil(null);
        }

        subscription.setBonusDaysEarned(subscription.getBonusDaysEarned() + days);
        subscriptionRepository.save(subscription);
    }

    public SubscriptionDTO.SubscriptionResponse toResponse(SubscriptionEntity subscription)
    {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime relevantEnd = switch (subscription.getStatus())
        {
            case TRIAL, TRIAL_ENDING -> subscription.getTrialEndsAt();
            case ACTIVE -> subscription.getCurrentPeriodEndsAt();
            case PAST_DUE -> subscription.getGraceUntil();
            case EXPIRED -> null;
        };

        Long daysRemaining = relevantEnd == null ? null : ChronoUnit.DAYS.between(now, relevantEnd);

        String warning = buildWarningMessage(subscription, daysRemaining);

        return SubscriptionDTO.SubscriptionResponse.builder()
                .shopId(subscription.getShop().getId())
                .plan(subscription.getPlan())
                .status(subscription.getStatus())
                .trialEndsAt(subscription.getTrialEndsAt())
                .currentPeriodEndsAt(subscription.getCurrentPeriodEndsAt())
                .graceUntil(subscription.getGraceUntil())
                .bonusDaysEarned(subscription.getBonusDaysEarned())
                .daysRemaining(daysRemaining)
                .accessBlocked(subscription.getStatus() == SubscriptionStatusEnum.EXPIRED)
                .warningMessage(warning)
                .build();
    }

    private String buildWarningMessage(SubscriptionEntity subscription, Long daysRemaining)
    {
        return switch (subscription.getStatus())
        {
            case TRIAL_ENDING -> daysRemaining != null && daysRemaining >= 0
                    ? String.format(
                            "Votre essai gratuit se termine dans %d jour(s). Abonnez-vous pour continuer à utiliser Sellam sans interruption.",
                            daysRemaining)
                    : "Votre essai gratuit se termine bientôt. Abonnez-vous pour continuer à utiliser Sellam sans interruption.";
            case PAST_DUE -> daysRemaining != null && daysRemaining >= 0
                    ? String.format(
                            "Votre abonnement a expiré. Vous avez encore %d jour(s) pour le renouveler avant blocage de l'accès.",
                            daysRemaining)
                    : "Votre abonnement a expiré. Renouvelez-le rapidement avant blocage de l'accès.";
            case EXPIRED -> "Votre accès est bloqué : essai ou abonnement expiré. Abonnez-vous pour continuer.";
            default -> null;
        };
    }
}
