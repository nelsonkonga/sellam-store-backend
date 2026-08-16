package com.sellam.store.sales.models;

import com.sellam.store.invoices.models.DiscountTypeEnum;
import com.sellam.store.invoices.models.InvoiceEntity;
import com.sellam.store.products.models.ProductEntity;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.users.models.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="sales")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class SaleEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private BigDecimal quantity;

    private BigDecimal totalPrice;

    private BigDecimal margin;

    @Enumerated(EnumType.STRING)
    private SaleStatusEnum status;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime soldAt;


    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "shop_id", nullable = false)
    private ShopEntity shop;

    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceEntity invoice;

    @Enumerated(EnumType.STRING)
    private DiscountTypeEnum discountType;

    private BigDecimal discountValue;

    private BigDecimal lineSubtotal;

    private BigDecimal discountAmount;

}
