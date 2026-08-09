package com.sellam.store.invoices.models;

import com.sellam.store.sales.models.SaleEntity;
import com.sellam.store.shops.models.ShopEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "invoices")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class InvoiceEntity
{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String invoiceNumber;

    @ManyToOne
    @JoinColumn(name = "shop_id", nullable = false)
    private ShopEntity shop;

    private String customerName;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
    private List<SaleEntity> lines;

    @Enumerated(EnumType.STRING)
    private DiscountTypeEnum discountType;
    private BigDecimal discountValue;

    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal totalMargin;

    @Enumerated(EnumType.STRING)
    private InvoiceStatusEnum status;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}