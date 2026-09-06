package com.sellam.store.payments.repositories;

import com.sellam.store.payments.models.PaymentTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, UUID>
{
    Optional<PaymentTransactionEntity> findByTransactionId(String transactionId);
}
