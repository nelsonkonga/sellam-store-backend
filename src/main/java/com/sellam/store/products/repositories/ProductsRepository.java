package com.sellam.store.products.repositories;

import com.sellam.store.products.models.ProductEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductEntity p WHERE p.id = :id")
    java.util.Optional<ProductEntity> findByIdForUpdate(UUID id);

@org.springframework.data.jpa.repository.Query(
    "SELECT p, " +
    "COALESCE(SUM(s.margin), 0) as totalMargin, " +
    "COALESCE(SUM(s.quantity), 0) as totalSold " +
    "FROM ProductEntity p " +
    "LEFT JOIN SaleEntity s ON s.product = p AND s.status = 'CONFIRMED' " +
    "WHERE p.shop.id = :shopId " +
    "GROUP BY p " +
    "ORDER BY totalMargin DESC"
)
List<Object[]> findTopSellingProductsByShopId(@org.springframework.data.repository.query.Param("shopId") UUID shopId);


    @org.springframework.data.jpa.repository.Query("SELECT p.name, p.stockQuantity, MAX(s.soldAt) " +
           "FROM ProductEntity p LEFT JOIN SaleEntity s ON s.product = p AND s.status = 'CONFIRMED' " +
           "WHERE p.shop.id = :shopId AND p.stockQuantity > 0 " +
           "GROUP BY p.name, p.stockQuantity")
    List<Object[]> findDeadStock(@org.springframework.data.repository.query.Param("shopId") UUID shopId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(p.id), COALESCE(SUM(p.stockQuantity), 0)
        FROM ProductEntity p
        WHERE p.shop.id = :shopId
          AND p.stockQuantity > 0
          AND (
              NOT EXISTS (
                  SELECT 1 FROM SaleEntity s
                  WHERE s.product = p AND s.status = 'CONFIRMED'
              )
              OR (
                  SELECT MAX(s.soldAt) FROM SaleEntity s
                  WHERE s.product = p AND s.status = 'CONFIRMED'
              ) < :cutoffDate
          )
    """)
    List<Object[]> getDormantStockSummary(
            @org.springframework.data.repository.query.Param("shopId") UUID shopId,
            @org.springframework.data.repository.query.Param("cutoffDate") java.time.LocalDateTime cutoffDate
    );

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM ProductEntity p WHERE p.shop.id = :shopId AND p.stockQuantity <= p.alertThreshold")
    long countCriticalStock(@org.springframework.data.repository.query.Param("shopId") UUID shopId);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM ProductEntity p WHERE p.shop.id = :shopId AND p.stockQuantity <= p.alertThreshold ORDER BY p.stockQuantity ASC")
    List<ProductEntity> findCriticalStockProducts(@org.springframework.data.repository.query.Param("shopId") UUID shopId, org.springframework.data.domain.Pageable pageable);
}
