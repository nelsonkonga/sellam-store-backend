package com.sellam.store.cash.repositories;

import com.sellam.store.cash.models.CashMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CashMovementRepository extends JpaRepository<CashMovementEntity, UUID> {

    List<CashMovementEntity> findBySessionIdOrderByTimestampAsc(UUID sessionId);

    /**
     * Somme nette des mouvements d'une session : entrées (VENTE_CASH, APPOINT) - sorties (RETRAIT, DEPENSE, REMBOURSEMENT).
     * CORRECTION peut être + ou -, donc on le traite de manière signée dans le service.
     * Cette requête retourne la somme brute de tous les montants (signés selon le type).
     */
    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN m.type IN (com.sellam.store.cash.models.CashMovementTypeEnum.VENTE_CASH, 
                                com.sellam.store.cash.models.CashMovementTypeEnum.APPOINT) THEN m.amount
                WHEN m.type IN (com.sellam.store.cash.models.CashMovementTypeEnum.RETRAIT, 
                                com.sellam.store.cash.models.CashMovementTypeEnum.DEPENSE, 
                                com.sellam.store.cash.models.CashMovementTypeEnum.REMBOURSEMENT) THEN -m.amount
                WHEN m.type = com.sellam.store.cash.models.CashMovementTypeEnum.CORRECTION THEN m.amount
                ELSE 0
            END
        ), 0) FROM CashMovementEntity m WHERE m.session.id = :sessionId
    """)
    BigDecimal computeNetMovements(@Param("sessionId") UUID sessionId);

    /**
     * Somme nette des mouvements d'une session jusqu'à un horodatage donné (inclus).
     */
    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN m.type IN (com.sellam.store.cash.models.CashMovementTypeEnum.VENTE_CASH, 
                                com.sellam.store.cash.models.CashMovementTypeEnum.APPOINT) THEN m.amount
                WHEN m.type IN (com.sellam.store.cash.models.CashMovementTypeEnum.RETRAIT, 
                                com.sellam.store.cash.models.CashMovementTypeEnum.DEPENSE, 
                                com.sellam.store.cash.models.CashMovementTypeEnum.REMBOURSEMENT) THEN -m.amount
                WHEN m.type = com.sellam.store.cash.models.CashMovementTypeEnum.CORRECTION THEN m.amount
                ELSE 0
            END
        ), 0) FROM CashMovementEntity m WHERE m.session.id = :sessionId AND m.timestamp <= :until
    """)
    BigDecimal computeNetMovementsUntil(@Param("sessionId") UUID sessionId, @Param("until") LocalDateTime until);

    List<CashMovementEntity> findByReferenceInvoiceId(UUID invoiceId);
}
