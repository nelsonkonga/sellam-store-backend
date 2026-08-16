package com.sellam.store.accounts.models;

import com.sellam.store.profile.models.ThemePreferenceEnum;
import com.sellam.store.shops.models.ShopEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name="accounts")
@Entity
public class AccountEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private String phoneNumber;

    private String email;

    @Builder.Default
    private boolean emailVerified = false;

    private String verificationToken;

    private LocalDateTime verificationTokenExpiresAt;

    private String resetToken;

    private LocalDateTime resetTokenExpiresAt;

    private String passwordHash;

    @Column(columnDefinition = "TEXT")
    private String profilePictureUrl;

    private String oauthProvider;

    private String oauthId;

    @Enumerated(EnumType.STRING)
    private ThemePreferenceEnum themePreference;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;


    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<ShopEntity> shops;

}
