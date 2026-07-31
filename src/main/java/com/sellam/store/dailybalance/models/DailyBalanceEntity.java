package com.sellam.store.dailybalance.models;

import com.sellam.store.shops.models.ShopEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "daily_balances")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class DailyBalanceEntity
{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "shop_id", nullable = false)
    private ShopEntity shop;

    private LocalDate balanceDate;

    private BigDecimal computedTotalSales;
    private BigDecimal computedTotalMargin;
    private BigDecimal declaredCash;
    private BigDecimal discrepancy;

    @Enumerated(EnumType.STRING)
    private BalanceStatusEnum status;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}