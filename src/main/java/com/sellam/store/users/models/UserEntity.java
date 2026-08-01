package com.sellam.store.users.models;

import com.sellam.store.profile.models.ThemePreferenceEnum;
import com.sellam.store.sales.models.SaleEntity;
import com.sellam.store.shops.models.ShopEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="users")
@Entity
public class UserEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private String phoneNumber;

    private String passwordHash;

    @Column(columnDefinition = "TEXT")
    private String profilePictureUrl;

    @Enumerated(EnumType.STRING)
    private RoleEnum role;

    private boolean active;

    private ThemePreferenceEnum themePreference;


    @ManyToOne
    @JoinColumn(name = "shop_id", nullable = false)
    private ShopEntity shop;

    @OneToMany(mappedBy = "user")
    private List<SaleEntity> sales;
}
