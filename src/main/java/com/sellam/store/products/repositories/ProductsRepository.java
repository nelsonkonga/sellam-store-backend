package com.sellam.store.products.repositories;

import com.sellam.store.products.models.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductsRepository extends JpaRepository<ProductEntity, UUID>
{
    ProductEntity findByNameAndCategory(String name, String category);

    List<ProductEntity> findByShop_Id(UUID shopId);
}
