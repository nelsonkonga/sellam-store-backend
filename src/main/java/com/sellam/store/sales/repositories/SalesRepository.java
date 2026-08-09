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
}