package com.sellam.store.accounts.models;

import com.sellam.store.profile.models.ThemePreferenceEnum;
import com.sellam.store.shops.models.ShopEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
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

    private String passwordHash;

    private String profilePictureUrl;

    private String oauthProvider;

    private String oauthId;

    private ThemePreferenceEnum themePreference;

    private LocalDateTime createdAt;


    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<ShopEntity> shops;

    public ShopEntity createShop(ShopEntity shop){
        return null;
    }

}
