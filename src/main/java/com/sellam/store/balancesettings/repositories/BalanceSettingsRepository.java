package com.sellam.store.balancesettings.repositories;

import com.sellam.store.balancesettings.models.BalanceSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BalanceSettingsRepository extends JpaRepository<BalanceSettingsEntity, UUID>
{
    List<BalanceSettingsEntity> findByShop_Id(UUID shopId);

    Optional<BalanceSettingsEntity> findByShop_IdAndDayOfWeek(UUID shopId, DayOfWeek dayOfWeek);

    // Pour le scheduler de rappels de bilan
    List<BalanceSettingsEntity> findByDayOfWeekAndEnabled(DayOfWeek dayOfWeek, boolean enabled);

    @Query("SELECT b FROM BalanceSettingsEntity b WHERE b.dayOfWeek = :dayOfWeek AND b.enabled = true AND :now BETWEEN b.openingTime AND b.closingTime")
    List<BalanceSettingsEntity> findActiveSettingsForTodayAndNow(
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("now") LocalTime now
    );
}