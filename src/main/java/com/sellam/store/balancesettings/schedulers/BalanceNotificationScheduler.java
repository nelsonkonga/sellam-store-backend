package com.sellam.store.balancesettings.schedulers;

import com.sellam.store.balancesettings.models.BalanceSettingsEntity;
import com.sellam.store.balancesettings.repositories.BalanceSettingsRepository;
import com.sellam.store.common.email.services.EmailService;
import com.sellam.store.notifications.models.NotificationEntity;
import com.sellam.store.notifications.repositories.NotificationRepository;
import com.sellam.store.notifications.services.PushNotificationService;
import com.sellam.store.users.models.RoleEnum;
import com.sellam.store.users.models.UserEntity;
import com.sellam.store.users.repositories.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Tâche programmée qui envoie des rappels de bilan aux boutiques.
 * S'exécute toutes les 5 minutes pour vérifier s'il faut envoyer des rappels.
 */
@Slf4j
@Service
@AllArgsConstructor
@Profile("!dev & !postgres")
public class BalanceNotificationScheduler {

    private final BalanceSettingsRepository balanceSettingsRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PushNotificationService pushNotificationService;

    /**
     * S'exécute au début de chaque minute pour envoyer les rappels de bilan.
     */
    @Scheduled(cron = "0 * * * * *") // A la seconde 0 de chaque minute
    public void sendBalanceReminders() {
        log.debug("Démarrage du scheduler de rappels de bilan");

        LocalDate today = LocalDate.now();
        DayOfWeek todayDayOfWeek = today.getDayOfWeek();
        LocalTime now = LocalTime.now();

        List<BalanceSettingsEntity> enabledSettingsForToday = balanceSettingsRepository
                .findByDayOfWeekAndEnabled(todayDayOfWeek, true);

        for (BalanceSettingsEntity setting : enabledSettingsForToday) {
            try {
                processBalanceReminder(setting, today, now);
            } catch (Exception e) {
                log.error("Erreur lors du traitement du rappel de bilan pour la boutique {}",
                        setting.getShop().getId(), e);
            }
        }

        log.debug("Fin du scheduler de rappels de bilan");
    }

    private void processBalanceReminder(BalanceSettingsEntity setting, LocalDate today, LocalTime now) {
        LocalTime openingTime = setting.getOpeningTime();
        LocalTime closingTime = setting.getClosingTime();
        Integer frequencyHours = setting.getReminderFrequencyHours();

        if (openingTime == null || closingTime == null || frequencyHours == null) {
            return;
        }

        if (now.isBefore(openingTime) || now.isAfter(closingTime)) {
            return;
        }

        LocalTime firstReminderTime = openingTime.plusHours(frequencyHours);
        LocalTime currentTimeRounded = LocalTime.of(now.getHour(), now.getMinute());
        LocalTime checkTime = firstReminderTime;
        
        while (checkTime.isBefore(closingTime) || checkTime.equals(closingTime)) {
            if (isTimeMatching(currentTimeRounded, checkTime, 2)) {
                if (!notificationAlreadySent(setting.getShop().getId(), today, checkTime)) {
                    sendReminder(setting, today, checkTime);
                }
            }
            checkTime = checkTime.plusHours(frequencyHours);
        }
    }

    private boolean isTimeMatching(LocalTime currentTime, LocalTime scheduledTime, int toleranceMinutes) {
        long currentMinutes = currentTime.getHour() * 60 + currentTime.getMinute();
        long scheduledMinutes = scheduledTime.getHour() * 60 + scheduledTime.getMinute();
        long diff = Math.abs(currentMinutes - scheduledMinutes);
        return diff <= toleranceMinutes;
    }

    private boolean notificationAlreadySent(java.util.UUID shopId, LocalDate date, LocalTime time) {
        return notificationRepository
                .findByShopIdAndNotificationTypeAndNotificationDateAndScheduledTime(
                        shopId,
                        NotificationEntity.NotificationType.BALANCE_REMINDER,
                        date,
                        time.toString()
                )
                .isPresent();
    }

    private void sendReminder(BalanceSettingsEntity setting, LocalDate date, LocalTime time) {
        try {
            // Collecte de tous les emails autorisés (Gérant principal + Employés)
            java.util.Set<String> recipientEmails = new java.util.HashSet<>();
            
            // 1. Email du compte principal (gérant)
            String accountEmail = setting.getShop().getAccount().getEmail();
            if (accountEmail != null && !accountEmail.isEmpty()) {
                recipientEmails.add(accountEmail);
            }
            
            // 2. Emails de tous les utilisateurs autorisés de la boutique
            List<UserEntity> shopUsers = userRepository.findByShop_Id(setting.getShop().getId());
            for (UserEntity user : shopUsers) {
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    recipientEmails.add(user.getEmail());
                }
            }

            // Envoi des emails
            for (String email : recipientEmails) {
                try {
                    emailService.sendBalanceReminderEmail(email, setting.getShop().getName());
                } catch (Exception e) {
                    log.error("Erreur lors de l'envoi de l'email à {}", email, e);
                }
            }

            // Envoi de la notification Push
            pushNotificationService.sendToShop(
                    setting.getShop().getId(),
                    "Rappel de bilan",
                    "Il est temps de faire le bilan pour " + setting.getShop().getName()
            );

            // Création d'une seule entrée de notification pour la boutique
            String recipientsStr = String.join(", ", recipientEmails);
            if (recipientsStr.length() > 255) {
                recipientsStr = recipientsStr.substring(0, 252) + "...";
            }

            NotificationEntity notification = NotificationEntity.builder()
                    .shopId(setting.getShop().getId())
                    .notificationType(NotificationEntity.NotificationType.BALANCE_REMINDER)
                    .notificationDate(date)
                    .scheduledTime(time.toString())
                    .recipientEmail(recipientsStr.isEmpty() ? "Aucun email configuré" : recipientsStr)
                    .title("Rappel de bilan")
                    .message("Il est temps de faire le bilan pour " + setting.getShop().getName())
                    .status(NotificationEntity.NotificationStatus.SENT)
                    .sentAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);
            log.info("Rappel de bilan envoyé (Email & Push) pour la boutique {}. Emails: {}", setting.getShop().getName(), recipientsStr);

        } catch (Exception e) {
            log.error("Erreur globale lors de l'envoi du rappel de bilan pour la boutique {}", setting.getShop().getId(), e);

            NotificationEntity failedNotification = NotificationEntity.builder()
                    .shopId(setting.getShop().getId())
                    .notificationType(NotificationEntity.NotificationType.BALANCE_REMINDER)
                    .notificationDate(date)
                    .scheduledTime(time.toString())
                    .status(NotificationEntity.NotificationStatus.FAILED)
                    .errorMessage(e.getMessage() != null && e.getMessage().length() > 255 ? e.getMessage().substring(0, 255) : e.getMessage())
                    .build();

            notificationRepository.save(failedNotification);
        }
    }
}

