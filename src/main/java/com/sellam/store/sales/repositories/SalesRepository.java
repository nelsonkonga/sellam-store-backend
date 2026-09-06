package com.sellam.store.sales.repositories;

import com.sellam.store.sales.models.SaleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SalesRepository extends JpaRepository<SaleEntity, UUID>
{
    List<SaleEntity> findByShop_IdAndSoldAtBetween(UUID shopId, LocalDateTime start, LocalDateTime end);
    List<SaleEntity> findByShop_IdAndSoldAtBetweenOrderBySoldAtDesc(UUID shopId, LocalDateTime start, LocalDateTime end);
    List<SaleEntity> findTop5ByShop_IdOrderBySoldAtDesc(UUID shopId);
    List<SaleEntity> findByInvoice_Id(UUID invoiceId);
    SaleEntity findByInvoice_IdAndProduct_Id(UUID invoiceId, UUID productId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(s.totalPrice), 0), COALESCE(SUM(s.margin), 0) FROM SaleEntity s WHERE s.invoice.id = :invoiceId")
    List<Object[]> computeInvoiceTotals(@org.springframework.data.repository.query.Param("invoiceId") UUID invoiceId);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM SaleEntity s WHERE s.shop.id = :shopId AND s.margin < 0 AND s.status = 'CONFIRMED' AND s.soldAt BETWEEN :start AND :end ORDER BY s.soldAt DESC")
    List<SaleEntity> findNegativeMarginSales(
            @org.springframework.data.repository.query.Param("shopId") UUID shopId,
            @org.springframework.data.repository.query.Param("start") LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") LocalDateTime end
    );

    @org.springframework.data.jpa.repository.Query("SELECT s.product.name, SUM(s.quantity), SUM(s.margin) FROM SaleEntity s WHERE s.shop.id = :shopId AND s.status = 'CONFIRMED' AND s.soldAt BETWEEN :start AND :end GROUP BY s.product.name")
    List<Object[]> getProductPerformance(
            @org.springframework.data.repository.query.Param("shopId") UUID shopId,
            @org.springframework.data.repository.query.Param("start") LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") LocalDateTime end
    );

    @org.springframework.data.jpa.repository.Query("SELECT s.product.category, SUM(s.totalPrice), SUM(s.margin), SUM(s.quantity) FROM SaleEntity s WHERE s.shop.id = :shopId AND s.status = 'CONFIRMED' AND s.soldAt BETWEEN :start AND :end GROUP BY s.product.category")
    List<Object[]> getCategoryPerformance(
            @org.springframework.data.repository.query.Param("shopId") UUID shopId,
            @org.springframework.data.repository.query.Param("start") LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") LocalDateTime end
    );

    @org.springframework.data.jpa.repository.Query("SELECT s.product.id, s.product.name, s.product.stockQuantity, SUM(s.quantity) FROM SaleEntity s WHERE s.shop.id = :shopId AND s.status = 'CONFIRMED' AND s.soldAt BETWEEN :start AND :end GROUP BY s.product.id, s.product.name, s.product.stockQuantity HAVING s.product.stockQuantity > 0")
    List<Object[]> getProductVelocity(
            @org.springframework.data.repository.query.Param("shopId") UUID shopId,
            @org.springframework.data.repository.query.Param("start") LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") LocalDateTime end
    );

    @org.springframework.data.jpa.repository.Query("SELECT s1.product.name, s2.product.name, COUNT(s1.id) FROM SaleEntity s1 JOIN SaleEntity s2 ON s1.invoice.id = s2.invoice.id AND s1.product.id < s2.product.id WHERE s1.shop.id = :shopId AND s1.status = 'CONFIRMED' AND s1.soldAt BETWEEN :start AND :end GROUP BY s1.product.name, s2.product.name HAVING COUNT(s1.id) >= 2 ORDER BY COUNT(s1.id) DESC")
    List<Object[]> getFrequentlyBoughtTogether(
            @org.springframework.data.repository.query.Param("shopId") UUID shopId,
            @org.springframework.data.repository.query.Param("start") LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") LocalDateTime end
    );

    @org.springframework.data.jpa.repository.Query("SELECT s.product.name, SUM(s.quantity) as qty, SUM(s.margin) as totalMargin FROM SaleEntity s WHERE s.shop.id = :shopId AND s.status = 'CONFIRMED' AND s.soldAt BETWEEN :start AND :end GROUP BY s.product.name ORDER BY totalMargin DESC")
    List<Object[]> getProductPerformanceOrderByMargin(
            @org.springframework.data.repository.query.Param("shopId") UUID shopId,
            @org.springframework.data.repository.query.Param("start") LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") LocalDateTime end
    );

    @org.springframework.data.jpa.repository.Query("SELECT s.product.name, SUM(s.quantity) as qty, SUM(s.margin) FROM SaleEntity s WHERE s.shop.id = :shopId AND s.status = 'CONFIRMED' AND s.soldAt BETWEEN :start AND :end GROUP BY s.product.name ORDER BY qty DESC")
    List<Object[]> getProductPerformanceOrderByQuantity(
            @org.springframework.data.repository.query.Param("shopId") UUID shopId,
            @org.springframework.data.repository.query.Param("start") LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") LocalDateTime end
    );
}