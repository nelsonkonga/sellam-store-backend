package com.sellam.store.invoices.repositories;

import com.sellam.store.invoices.models.InvoiceHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceHistoryRepository extends JpaRepository<InvoiceHistoryEntity, UUID>
{
    List<InvoiceHistoryEntity> findByInvoice_IdOrderByTimestampDesc(UUID invoiceId);
}
