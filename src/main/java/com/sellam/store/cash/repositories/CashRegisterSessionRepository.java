package com.sellam.store.cash.repositories;

import com.sellam.store.cash.models.CashRegisterSessionEntity;
import com.sellam.store.cash.models.CashSessionStatusEnum;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashRegisterSessionRepository extends JpaRepository<CashRegisterSessionEntity, UUID> {

    /**
     * Trouve la session active (OPEN ou PENDING_INITIAL_CASH) pour une caisse donnée.
     */
    @Query("SELECT s FROM CashRegisterSessionEntity s WHERE s.register.id = :registerId AND s.status IN :statuses")
    Optional<CashRegisterSessionEntity> findActiveSession(
            @Param("registerId") UUID registerId,
            @Param("statuses") List<CashSessionStatusEnum> statuses
    );

    /**
     * Trouve la session active (OPEN ou PENDING_INITIAL_CASH) pour une caisse donnée — avec verrou pessimiste.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM CashRegisterSessionEntity s WHERE s.register.id = :registerId AND s.status IN :statuses")
    Optional<CashRegisterSessionEntity> findActiveSessionForUpdate(
            @Param("registerId") UUID registerId,
            @Param("statuses") List<CashSessionStatusEnum> statuses
    );

    /**
     * Liste les sessions d'une caisse, triées par date d'ouverture décroissante.
     */
    List<CashRegisterSessionEntity> findByRegisterIdOrderByOpenedAtDesc(UUID registerId);

    /**
     * Liste les sessions d'une boutique (via le register), triées par date d'ouverture décroissante.
     */
    @Query("SELECT s FROM CashRegisterSessionEntity s WHERE s.register.shop.id = :shopId ORDER BY s.openedAt DESC")
    List<CashRegisterSessionEntity> findByShopIdOrderByOpenedAtDesc(@Param("shopId") UUID shopId);

    /**
     * Sessions PENDING_HANDOVER_CLOSURE pour une caisse donnée.
     */
    List<CashRegisterSessionEntity> findByRegisterIdAndStatus(UUID registerId, CashSessionStatusEnum status);
}
