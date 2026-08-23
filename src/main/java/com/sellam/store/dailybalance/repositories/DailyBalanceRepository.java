package com.sellam.store.dailybalance.repositories;

import com.sellam.store.dailybalance.models.DailyBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyBalanceRepository extends JpaRepository<DailyBalanceEntity, UUID>
{
    Optional<DailyBalanceEntity> findByShop_IdAndBalanceDate(UUID shopId, LocalDate balanceDate);

    List<DailyBalanceEntity> findByShop_IdOrderByBalanceDateDesc(UUID shopId);

    List<DailyBalanceEntity> findByShop_IdAndBalanceDateBetweenOrderByBalanceDateAsc(UUID shopId, LocalDate start, LocalDate end);
}