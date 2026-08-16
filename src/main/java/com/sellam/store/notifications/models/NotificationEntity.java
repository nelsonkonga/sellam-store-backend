package com.sellam.store.notifications.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Enregistre les notifications envoyées (rappels de bilan, etc.)
 * pour éviter les doublons et suivre l'historique.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_shop_day", columnList = "shop_id,notification_date"),
        @Index(name = "idx_type_sent", columnList = "notification_type,sent_at")
})
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID shopId;

    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    // Date du jour pour laquelle la notification a été envoyée
    private LocalDate notificationDate;

    // Heure programmée de cette notification (ex: 12h30)
    private String scheduledTime;

    // Contenu et destinataire
    private String recipientEmail;

    private String title;

    private String message;

    // Statut d'envoi
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private LocalDateTime sentAt;

    private String errorMessage;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum NotificationType {
        BALANCE_REMINDER, // Rappel de faire le bilan
        OTHER
    }

    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED
    }
}
