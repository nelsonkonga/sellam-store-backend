package com.sellam.store.invoices.models;

import com.sellam.store.shops.models.ShopEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "invoice_counters")
@Entity
public class InvoiceCounterEntity
{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "shop_id", nullable = false, unique = true)
    private ShopEntity shop;

    private int lastNumber;
}