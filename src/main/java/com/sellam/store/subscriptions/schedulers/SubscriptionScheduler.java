package com.sellam.store.subscriptions.schedulers;

import com.sellam.store.common.email.services.EmailService;
import com.sellam.store.identity.models.ShopMembershipEntity;
import com.sellam.store.identity.repositories.ShopMembershipRepository;
import com.sellam.store.subscriptions.models.SubscriptionEntity;
import com.sellam.store.subscriptions.models.SubscriptionStatusEnum;
import com.sellam.store.subscriptions.repositories.SubscriptionRepository;
import com.sellam.store.subscriptions.services.SubscriptionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Recalcule périodiquement le statut de chaque abonnement (pour ne pas
 * dépendre uniquement d'un accès utilisateur pour détecter une expiration)
 * et envoie les emails d'avertissement façon Shopify :
 *
 * - Un email dès l'entrée en TRIAL_ENDING (J-warningBeforeDays)
 * - Un email dès l'entrée en PAST_DUE (paiement expiré, période de grâce)
 * - Un email dès le passage en EXPIRED (accès bloqué)
 *
 * On évite les envois répétés en s'appuyant sur lastWarningSentAt : un seul
 * email par transition de statut, pas un par exécution du scheduler.
 */
@Slf4j
@Service
@AllArgsConstructor
public class SubscriptionScheduler
{
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final ShopMembershipRepository shopMembershipRepository;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 * * * *") // toutes les heures, à la minute 0
    @Transactional
    public void recalculateAndWarn()
    {
        log.debug("Démarrage du scheduler d'abonnements");

        List<SubscriptionEntity> candidates = subscriptionRepository.findByStatusIn(List.of(
                SubscriptionStatusEnum.TRIAL,
                SubscriptionStatusEnum.TRIAL_ENDING,
                SubscriptionStatusEnum.ACTIVE,
                SubscriptionStatusEnum.PAST_DUE
        ));
        LocalDateTime now = LocalDateTime.now();

        for (SubscriptionEntity subscription : candidates)
        {
            try
            {
                SubscriptionStatusEnum before = subscription.getStatus();
                subscriptionService.recalculate(subscription);
                SubscriptionStatusEnum after = subscription.getStatus();

                if (before != after)
                {
                    subscription.setLastWarningSentAt(now);
                    subscriptionRepository.save(subscription);
                    notifyStatusChange(subscription, after);
                }
            }
            catch (Exception e)
            {
                log.error("Erreur lors du traitement de l'abonnement de la boutique {}",
                        subscription.getShop().getId(), e);
            }
        }

        log.debug("Fin du scheduler d'abonnements");
    }

    private void notifyStatusChange(SubscriptionEntity subscription, SubscriptionStatusEnum newStatus)
    {
        if (newStatus != SubscriptionStatusEnum.TRIAL_ENDING
                && newStatus != SubscriptionStatusEnum.PAST_DUE
                && newStatus != SubscriptionStatusEnum.EXPIRED)
        {
            return;
        }

        Set<String> recipientEmails = new HashSet<>();
        List<ShopMembershipEntity> memberships =
                shopMembershipRepository.findByShopId(subscription.getShop().getId());
        for (ShopMembershipEntity membership : memberships)
        {
            String email = membership.getPerson().getEmail();
            if (email != null && !email.isBlank())
            {
                recipientEmails.add(email);
            }
        }

        String shopName = subscription.getShop().getName();
        for (String email : recipientEmails)
        {
            try
            {
                switch (newStatus)
                {
                    case TRIAL_ENDING -> emailService.sendSubscriptionTrialEndingEmail(email, shopName);
                    case PAST_DUE -> emailService.sendSubscriptionPastDueEmail(email, shopName);
                    case EXPIRED -> emailService.sendSubscriptionExpiredEmail(email, shopName);
                    default -> { /* rien */ }
                }
            }
            catch (Exception e)
            {
                log.error("Erreur lors de l'envoi de l'email d'abonnement à {}", email, e);
            }
        }
    }
}
