package com.sellam.store.users.models;

import com.sellam.store.profile.models.ThemePreferenceEnum;
import com.sellam.store.sales.models.SaleEntity;
import com.sellam.store.shops.models.ShopEntity;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="users")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class UserEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private String phoneNumber;

    private String email; // Email pour les notifications et vérifications

    private String passwordHash;

    @Column(columnDefinition = "TEXT")
    private String profilePictureUrl;

    @Enumerated(EnumType.STRING)
    private RoleEnum role;

    private boolean active;

    private ThemePreferenceEnum themePreference;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "shop_id", nullable = false)
    private ShopEntity shop;

    @OneToMany(mappedBy = "user")
    private List<SaleEntity> sales;

    /** Permissions ajoutées en plus de celles du rôle. */
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_permission_overrides", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "permission")
    @Builder.Default
    private Set<PermissionEnum> grantedOverrides = new HashSet<>();

    /** Permissions retirées de celles du rôle. */
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_permission_revokes", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "permission")
    @Builder.Default
    private Set<PermissionEnum> revokedOverrides = new HashSet<>();

    /**
     * Calcule l'ensemble effectif des permissions de cet utilisateur :
     * permissions par défaut du rôle + overrides accordés - overrides retirés.
     *
     * Défensif contre grantedOverrides/revokedOverrides null : @Builder.Default
     * ne protège que le chemin UserEntity.builder(), pas le chargement Hibernate
     * ni une construction via le constructeur généré par @AllArgsConstructor.
     */
    @Transient
    public Set<PermissionEnum> getEffectivePermissions()
    {
        Set<PermissionEnum> effective = EnumSet.noneOf(PermissionEnum.class);
        effective.addAll(RoleDefaultPermissions.forRole(this.role));
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

    @Transient
    public boolean hasPermission(PermissionEnum permission)
    {
        return getEffectivePermissions().contains(permission);
    }
}