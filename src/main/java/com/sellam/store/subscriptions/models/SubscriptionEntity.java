package com.sellam.store.subscriptions.models;

import com.sellam.store.shops.models.ShopEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Un abonnement par boutique (relation 1-1 avec ShopEntity).
 *
 * - trialEndsAt : date de fin d'essai gratuit, fixée à la création de la boutique.
 * - currentPeriodEndsAt : date de fin de la période actuellement payée
 *   (null tant qu'aucun paiement n'a jamais été confirmé).
 * - status : dérivé/maintenu par SubscriptionService, ne pas modifier
 *   directement en dehors du service (cf. recalculateStatus()).
 * - graceUntil : fin de la période de grâce après expiration du paiement,
 *   avant blocage effectif (PAST_DUE -> EXPIRED).
 * - firstPaymentConfirmedAt : posé au tout premier paiement confirmé.
 *   Sert de déclencheur pour la récompense de parrainage : le parrain n'est
 *   récompensé que lorsque le filleul devient payant pour la première fois.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "subscriptions", uniqueConstraints = {
        @UniqueConstraint(columnNames = "shop_id", name = "uk_subscription_shop")
})
@Entity
@EntityListeners(AuditingEntityListener.class)
public class SubscriptionEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private ShopEntity shop;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false)
    @Builder.Default
    private SubscriptionPlanEnum plan = SubscriptionPlanEnum.FREE_TRIAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SubscriptionStatusEnum status = SubscriptionStatusEnum.TRIAL;

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "current_period_ends_at")
    private LocalDateTime currentPeriodEndsAt;

    @Column(name = "grace_until")
    private LocalDateTime graceUntil;

    @Column(name = "first_payment_confirmed_at")
    private LocalDateTime firstPaymentConfirmedAt;

    /**
     * Jours bonus accumulés (parrainage, gestes commerciaux, etc.), ajoutés
     * à currentPeriodEndsAt/trialEndsAt selon le cas au moment de l'octroi
     * plutôt que stockés séparément — ce compteur ne sert qu'à l'historique
     * et à l'affichage ("vous avez gagné X jours grâce au parrainage").
     */
    @Column(name = "bonus_days_earned")
    @Builder.Default
    private Integer bonusDaysEarned = 0;

    @Column(name = "last_warning_sent_at")
    private LocalDateTime lastWarningSentAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
