package com.sellam.store.referrals.models;

import com.sellam.store.identity.models.PersonEntity;
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
 * Lien de parrainage entre un parrain (referrer) et un filleul (referee).
 * Créé au moment de l'inscription du filleul (register()), une seule fois
 * par filleul (un filleul n'a qu'un seul parrain, contrainte unique sur
 * referee).
 *
 * Ne contient PAS la récompense elle-même : cf. ReferralRewardEntity,
 * créée seulement quand le filleul devient payant pour la première fois
 * (cf. discussion produit : récompense déclenchée par le premier paiement,
 * pas par la simple inscription).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "referral_links", uniqueConstraints = {
        @UniqueConstraint(columnNames = "referee_id", name = "uk_referral_referee")
})
@Entity
@EntityListeners(AuditingEntityListener.class)
public class ReferralLinkEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrer_id", nullable = false)
    private PersonEntity referrer;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referee_id", nullable = false)
    private PersonEntity referee;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
