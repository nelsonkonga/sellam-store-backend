package com.sellam.store.balancesettings.repositories;

import com.sellam.store.balancesettings.models.BalanceSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BalanceSettingsRepository extends JpaRepository<BalanceSettingsEntity, UUID>
{
    List<BalanceSettingsEntity> findByShop_Id(UUID shopId);

    Optional<BalanceSettingsEntity> findByShop_IdAndDayOfWeek(UUID shopId, DayOfWeek dayOfWeek);
}