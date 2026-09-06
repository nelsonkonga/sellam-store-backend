package com.sellam.store.referrals.models;

import com.sellam.store.identity.models.PersonEntity;
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
 * Une récompense en attente ou déjà appliquée pour un parrain, déclenchée
 * par le premier paiement d'un filleul.
 *
 * appliedToShop : null tant que PENDING (parrain avec plusieurs boutiques,
 * doit choisir), renseigné dès APPLIED.
 *
 * bonusDays : capturé au moment de la création plutôt que relu depuis la
 * config à l'application, pour ne pas changer rétroactivement la valeur
 * d'une récompense déjà promise si la config évolue plus tard.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "referral_rewards")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class ReferralRewardEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrer_id", nullable = false)
    private PersonEntity referrer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referee_id", nullable = false)
    private PersonEntity referee;

    @Column(name = "bonus_days", nullable = false)
    private Integer bonusDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ReferralRewardStatusEnum status = ReferralRewardStatusEnum.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_to_shop_id")
    private ShopEntity appliedToShop;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
