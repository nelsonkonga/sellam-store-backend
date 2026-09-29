package com.sellam.store.identity.models;

import com.sellam.store.profile.models.ThemePreferenceEnum;
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
 * Entité unifiée représentant une identité humaine unique.
 * Fusionne les concepts précédents de AccountEntity et UserEntity.
 * 
 * Champs d'identification :
 * - phoneNumber : nullable, unique si renseigné
 * - email : nullable, unique si renseigné
 * - Au moins l'un des deux est requis (validé en application et en base)
 * 
 * Rôles :
 * - systemRole : rôle plateforme (PLATFORM_ADMIN ou null), distinct des rôles métier
 * - Les rôles métier (MANAGER, CASHIER, SECRETARY) sont gérés via ShopMembershipEntity
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name="persons",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "phone_number", name = "uk_person_phone"),
           @UniqueConstraint(columnNames = "email", name = "uk_person_email")
       })
@Entity
public class PersonEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @Column(unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "profile_picture_url", columnDefinition = "TEXT")
    private String profilePictureUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme_preference")
    private ThemePreferenceEnum themePreference;

    // OAuth fields - conservés depuis AccountEntity
    @Column(name = "oauth_provider")
    private String oauthProvider;

    @Column(name = "oauth_id")
    private String oauthId;

    // Rôle système plateforme (distinct des rôles métier par boutique)
    @Enumerated(EnumType.STRING)
    @Column(name = "system_role")
    private SystemRoleEnum systemRole;

    // Trackers pour les limites de changement de contact (3 mois)
    @Column(name = "last_phone_change_at")
    private LocalDateTime lastPhoneChangeAt;

    @Column(name = "last_email_change_at")
    private LocalDateTime lastEmailChangeAt;

    // Email verification - conservé depuis AccountEntity
    @Builder.Default
    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "referral_code", unique = true)
    private String referralCode;

    @Column(name = "verification_token_expires_at")
    private LocalDateTime verificationTokenExpiresAt;

    // Password reset - conservé depuis AccountEntity
    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expires_at")
    private LocalDateTime resetTokenExpiresAt;

    @Builder.Default
    @Column(name = "token_version", nullable = false)
    private int tokenVersion = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isEmailVerified() {
        return Boolean.TRUE.equals(this.emailVerified);
    }
}
