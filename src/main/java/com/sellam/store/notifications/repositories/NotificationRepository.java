package com.sellam.store.notifications.repositories;

import com.sellam.store.notifications.models.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findByShopIdOrderByCreatedAtDesc(UUID shopId);

    /**
     * Trouve une notification de rappel de bilan pour une boutique à une date/heure donnée
     * pour éviter les doublons.
     */
    Optional<NotificationEntity> findByShopIdAndNotificationTypeAndNotificationDateAndScheduledTime(
            UUID shopId,
            NotificationEntity.NotificationType type,
            LocalDate date,
            String scheduledTime
    );

    /**
     * Récupère toutes les notifications de rappel de bilan pour une boutique à une date donnée.
     */
    List<NotificationEntity> findByShopIdAndNotificationTypeAndNotificationDate(
            UUID shopId,
            NotificationEntity.NotificationType type,
            LocalDate date
    );

    /**
     * Récupère les notifications en attente d'envoi.
     */
    List<NotificationEntity> findByStatus(NotificationEntity.NotificationStatus status);
}
