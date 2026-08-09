package com.sellam.store.invoices.repositories;

import com.sellam.store.invoices.models.InvoiceCounterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceCounterRepository extends JpaRepository<InvoiceCounterEntity, UUID>
{
    Optional<InvoiceCounterEntity> findByShop_Id(UUID shopId);
}