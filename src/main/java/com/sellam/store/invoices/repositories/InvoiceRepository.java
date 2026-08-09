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
}