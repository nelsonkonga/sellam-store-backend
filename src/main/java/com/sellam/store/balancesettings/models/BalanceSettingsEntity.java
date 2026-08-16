package com.sellam.store.balancesettings.models;

import com.sellam.store.shops.models.ShopEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "balance_settings", uniqueConstraints = @UniqueConstraint(columnNames = {"shop_id", "day_of_week"}))
@Entity
public class BalanceSettingsEntity
{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "shop_id", nullable = false)
    private ShopEntity shop;

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    private LocalTime balanceTime;

    private Integer reminderFrequencyHours;

    @Builder.Default
    private boolean enabled = true; // Par défaut, tous les jours sont actifs

    private LocalTime openingTime; // Heure d'ouverture de la boutique ce jour

    private LocalTime closingTime; // Heure de fermeture de la boutique ce jour
}