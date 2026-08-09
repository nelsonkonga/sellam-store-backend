package com.sellam.store.shops.models;

import com.sellam.store.accounts.models.AccountEntity;
import com.sellam.store.products.models.ProductEntity;
import com.sellam.store.users.models.UserEntity;
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
@Table(name="shops")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class ShopEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private String address;

    @Column(columnDefinition = "TEXT")
    private String logoUrl;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private Boolean autoPrintInvoices = false;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL)
    private List<UserEntity> users;

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL)
    private List<ProductEntity> products;




    public ProductEntity addProduct(ProductEntity product){
        return null;
    }


    public List<ProductEntity> listProduct(){
        return null;
    }

}
