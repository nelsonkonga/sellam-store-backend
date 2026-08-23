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

    java.util.Optional<ProductEntity> findByShop_IdAndBarcode(UUID shopId, String barcode);

    ProductEntity findByShop_IdAndNameAndCategory(UUID shopId, String name, String category);

    @org.springframework.data.jpa.repository.Query("SELECT p, COALESCE(SUM(s.quantity), 0) as totalSold " +
           "FROM ProductEntity p LEFT JOIN SaleEntity s ON s.product = p AND s.status = 'CONFIRMED' " +
           "WHERE p.shop.id = :shopId " +
           "GROUP BY p " +
           "ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProductsByShopId(@org.springframework.data.repository.query.Param("shopId") UUID shopId);

    @org.springframework.data.jpa.repository.Query("SELECT p.name, p.stockQuantity, MAX(s.soldAt) " +
           "FROM ProductEntity p LEFT JOIN SaleEntity s ON s.product = p AND s.status = 'CONFIRMED' " +
           "WHERE p.shop.id = :shopId AND p.stockQuantity > 0 " +
           "GROUP BY p.name, p.stockQuantity")
    List<Object[]> findDeadStock(@org.springframework.data.repository.query.Param("shopId") UUID shopId);
}
