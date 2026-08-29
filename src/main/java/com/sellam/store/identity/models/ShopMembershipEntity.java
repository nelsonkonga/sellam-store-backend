package com.sellam.store.identity.models;

import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.users.models.PermissionEnum;
import com.sellam.store.users.models.RoleEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Représente l'appartenance d'une personne à une boutique avec un rôle métier.
 * Remplace la relation directe UserEntity.shop en permettant les appartenances multiples.
 * 
 * Une personne peut appartenir à plusieurs boutiques avec des rôles différents :
 * - MANAGER dans la boutique A
 * - CASHIER dans la boutique B
 * - SECRETARY dans la boutique C
 * 
 * Les permissions spécifiques par membership peuvent être surchargées via
 * grantedOverrides et revokedOverrides, similaire au système existant dans UserEntity.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name="shop_memberships",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"person_id", "shop_id"}, name = "uk_person_shop")
       })
@Entity
public class ShopMembershipEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "person_id", nullable = false)
    private PersonEntity person;

    @ManyToOne
    @JoinColumn(name = "shop_id", nullable = false)
    private ShopEntity shop;

    /**
     * Rôle métier au sein de cette boutique (MANAGER, CASHIER, SECRETARY).
     * Distinct de systemRole qui est au niveau plateforme.
     */
    @Enumerated(EnumType.STRING)
    private RoleEnum role;

    /**
     * Indique si cette membership est active.
     * Permet de désactiver temporairement l'accès sans supprimer la relation.
     */
    @Builder.Default
    private boolean active = true;

    /**
     * Date d'admission dans cette boutique.
     */
    @CreatedDate
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    /**
     * Permissions accordées en plus de celles du rôle pour cette boutique spécifique.
     * Similarité avec UserEntity.grantedOverrides.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "membership_permission_grants",
                     joinColumns = @JoinColumn(name = "membership_id"))
    @Column(name = "permission")
    @Builder.Default
    private Set<PermissionEnum> grantedOverrides = new HashSet<>();

    /**
     * Permissions retirées de celles du rôle pour cette boutique spécifique.
     * Similarité avec UserEntity.revokedOverrides.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "membership_permission_revokes",
                     joinColumns = @JoinColumn(name = "membership_id"))
    @Column(name = "permission")
    @Builder.Default
    private Set<PermissionEnum> revokedOverrides = new HashSet<>();

    /**
     * Calcule l'ensemble effectif des permissions pour cette membership :
     * permissions par défaut du rôle + overrides accordés - overrides retirés.
     *
     * Défensif contre grantedOverrides/revokedOverrides null.
     */
    @Transient
    public Set<PermissionEnum> getEffectivePermissions()
    {
        Set<PermissionEnum> effective = EnumSet.noneOf(PermissionEnum.class);
        effective.addAll(com.sellam.store.users.models.RoleDefaultPermissions.forRole(this.role));
        if (this.grantedOverrides != null)
        {
            effective.addAll(this.grantedOverrides);
        }
        if (this.revokedOverrides != null)
        {
            effective.removeAll(this.revokedOverrides);
        }
        return effective;
    }

    /**
     * Vérifie si cette membership accorde une permission spécifique.
     */
    @Transient
    public boolean hasPermission(PermissionEnum permission)
    {
        return getEffectivePermissions().contains(permission);
    }
}
