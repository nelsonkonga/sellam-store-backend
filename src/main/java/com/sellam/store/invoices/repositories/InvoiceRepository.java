package com.sellam.store.invoices.repositories;

import com.sellam.store.invoices.models.InvoiceEntity;
import com.sellam.store.invoices.models.InvoiceStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID>
{
    List<InvoiceEntity> findByShop_IdAndStatusOrderByCreatedAtDesc(UUID shopId,
                                                                   InvoiceStatusEnum status
                                                                    );
    List<InvoiceEntity> findByShop_IdAndStatusAndCreatedAtBetween(
            UUID shopId,
            InvoiceStatusEnum status,
            LocalDateTime start,
            LocalDateTime end
            );

    @org.springframework.data.jpa.repository.Query("""
        SELECT COALESCE(SUM(i.totalAmount), 0),
               COALESCE(SUM(i.totalMargin), 0),
               COUNT(i.id),
               COALESCE(AVG(i.totalAmount), 0)
        FROM InvoiceEntity i
        WHERE i.shop.id = :shopId
          AND i.status = com.sellam.store.invoices.models.InvoiceStatusEnum.VALIDATED
          AND i.createdAt BETWEEN :start AND :end
    """)
    List<Object[]> getSummaryReportData(
            @org.springframework.data.repository.query.Param("shopId") UUID shopId,
            @org.springframework.data.repository.query.Param("start") LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") LocalDateTime end
    );

    @org.springframework.data.jpa.repository.Query("SELECT i.paymentMethod, SUM(i.totalAmount), COUNT(i.id) FROM InvoiceEntity i WHERE i.shop.id = :shopId AND i.status = 'VALIDATED' AND i.createdAt BETWEEN :start AND :end GROUP BY i.paymentMethod")
    List<Object[]> getPaymentMethodPerformance(
            @org.springframework.data.repository.query.Param("shopId") UUID shopId,
            @org.springframework.data.repository.query.Param("start") LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") LocalDateTime end
    );

    @org.springframework.data.jpa.repository.Query("SELECT i.validatedByName, SUM(i.totalAmount), COUNT(i.id) FROM InvoiceEntity i WHERE i.shop.id = :shopId AND i.status = 'VALIDATED' AND i.createdAt BETWEEN :start AND :end GROUP BY i.validatedByName")
    List<Object[]> getEmployeePerformance(
            @org.springframework.data.repository.query.Param("shopId") UUID shopId,
            @org.springframework.data.repository.query.Param("start") LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") LocalDateTime end
    );

    @org.springframework.data.jpa.repository.Query("SELECT i.customerName, SUM(i.totalAmount) AS totalRevenue, COUNT(i.id) FROM InvoiceEntity i WHERE i.shop.id = :shopId AND i.status = 'VALIDATED' AND i.customerName IS NOT NULL AND i.createdAt BETWEEN :start AND :end GROUP BY i.customerName ORDER BY SUM(i.totalAmount) DESC")
    List<Object[]> getCustomerPerformance(
            @org.springframework.data.repository.query.Param("shopId") UUID shopId,
            @org.springframework.data.repository.query.Param("start") LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") LocalDateTime end
    );

    @org.springframework.data.jpa.repository.Query("SELECT CAST(i.createdAt AS LocalDate), SUM(i.totalAmount) FROM InvoiceEntity i WHERE i.shop.id = :shopId AND i.status = 'VALIDATED' AND i.createdAt BETWEEN :start AND :end GROUP BY CAST(i.createdAt AS LocalDate) ORDER BY CAST(i.createdAt AS LocalDate)")
    List<Object[]> getDailyRevenue(
            @org.springframework.data.repository.query.Param("shopId") UUID shopId,
            @org.springframework.data.repository.query.Param("start") LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") LocalDateTime end
    );

    @org.springframework.data.jpa.repository.Query("SELECT i FROM InvoiceEntity i WHERE i.shop.id = :shopId AND i.status = com.sellam.store.invoices.models.InvoiceStatusEnum.VALIDATED AND i.createdAt BETWEEN :start AND :end ORDER BY i.createdAt DESC")
    List<InvoiceEntity> findTodayInvoices(
            @org.springframework.data.repository.query.Param("shopId") UUID shopId,
            @org.springframework.data.repository.query.Param("start") LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") LocalDateTime end,
            org.springframework.data.domain.Pageable pageable
    );
}