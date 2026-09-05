package com.sellam.store.cash.repositories;

import com.sellam.store.cash.models.CashRegisterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashRegisterRepository extends JpaRepository<CashRegisterEntity, UUID> {

    List<CashRegisterEntity> findByShopIdAndActiveTrue(UUID shopId);

    Optional<CashRegisterEntity> findFirstByShopIdAndActiveTrue(UUID shopId);

    long countByShopIdAndActiveTrue(UUID shopId);
}
