package com.sellam.store.products.models;

import com.sellam.store.sales.models.SaleEntity;
import com.sellam.store.sales.models.SaleTypeEnum;
import com.sellam.store.shops.models.ShopEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="products")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class ProductEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private String barcode;

    private String pictureUrl;

    @Enumerated(EnumType.STRING)
    private SaleTypeEnum saleTypeEnum;

    private BigDecimal purchasePrice;

    private BigDecimal sellingPrice;

    private BigDecimal stockQuantity;

    private BigDecimal alertThreshold;

    private String category;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDate expirationDate;


    @ManyToOne
    @JoinColumn(name = "shop_id", nullable = false)
    private ShopEntity shop;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<SaleEntity> sales;

    public SaleEntity registerSale(BigDecimal quantity)
    {
        return null;
    }

    public boolean isLowStock()
    {
        return false;
    }

    public BigDecimal calculateMargin(BigDecimal quantity)
    {
        return null;
    }




}
