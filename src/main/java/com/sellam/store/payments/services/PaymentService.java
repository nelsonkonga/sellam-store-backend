package com.sellam.store.payments.services;

import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.models.ShopMembershipEntity;
import com.sellam.store.identity.repositories.ShopMembershipRepository;
import com.sellam.store.payments.dto.PaymentDTO;
import com.sellam.store.payments.models.PaymentTransactionEntity;
import com.sellam.store.payments.models.PaymentTransactionStatusEnum;
import com.sellam.store.payments.repositories.PaymentTransactionRepository;
import com.sellam.store.referrals.services.ReferralService;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import com.sellam.store.subscriptions.services.SubscriptionService;
import com.sellam.store.payments.exceptions.PaymentProviderUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Orchestration du paiement d'abonnement via CinetPay.
 *
 * Flux :
 * 1) initiatePayment : crée une PaymentTransactionEntity PENDING, appelle
 *    CinetPay /payment, renvoie le payment_token au frontend pour ouvrir
 *    le widget Seamless.
 * 2) CinetPay notifie notify_url en POST (souvent plusieurs fois, et sans
 *    garantie d'ordre) -> handleNotification.
 * 3) handleNotification NE FAIT JAMAIS confiance au corps du POST reçu :
 *    il relit transaction_id, puis rappelle CinetPay /payment/check pour
 *    obtenir le statut réel, avant de confirmer quoi que ce soit.
 * 4) Idempotence : si la transaction est déjà ACCEPTED en base, on
 *    n'applique pas une seconde fois confirmPayment/récompense parrainage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService
{
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ShopRepository shopRepository;
    private final ShopMembershipRepository shopMembershipRepository;
    private final CinetPayClient cinetPayClient;
    private final SubscriptionService subscriptionService;
    private final ReferralService referralService;

    @Value("${app.subscription.plan-amount-xaf}")
    private int planAmount;

    @Value("${cinetpay.site-id:${payment-integration.site-id}}")
    private String siteId;

    @Value("${cinetpay.currency:${payment-integration.currency}}")
    private String currency;

    @Transactional
    public PaymentDTO.InitiatePaymentResponse initiatePayment(UUID shopId)
    {
        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique introuvable"));

        // On récupère un gérant de la boutique pour le nom/email/téléphone
        // exigés par CinetPay (customer_name, etc.) : le premier MANAGER
        // actif suffit, ce ne sont que des métadonnées d'affichage côté
        // CinetPay, pas une donnée métier stockée par nous.
        List<ShopMembershipEntity> memberships = shopMembershipRepository.findByShopId(shopId);
        PersonEntity payer = memberships.stream()
                .map(ShopMembershipEntity::getPerson)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Aucun membre trouvé pour cette boutique"));

        String transactionId = "SUB-" + shopId + "-" + System.currentTimeMillis();

        PaymentTransactionEntity transaction = PaymentTransactionEntity.builder()
                .transactionId(transactionId)
                .shop(shop)
                .amount(planAmount)
                .currency(currency)
                .status(PaymentTransactionStatusEnum.PENDING)
                .build();
        paymentTransactionRepository.save(transaction);

        PaymentDTO.CinetPayInitResponse initResponse;
        try
        {
            initResponse = cinetPayClient.initPayment(
                    transactionId,
                    planAmount,
                    "Abonnement Sellam - " + shop.getName(),
                    payer.getName(),
                    payer.getEmail(),
                    payer.getPhoneNumber()
            );
        }
        catch (Exception e)
        {
            log.error("Erreur réseau/API lors de l'appel à CinetPay pour la transaction {}", transactionId, e);
            transaction.setStatus(PaymentTransactionStatusEnum.FAILED);
            paymentTransactionRepository.save(transaction);
            throw new PaymentProviderUnavailableException(
                    "Le paiement en ligne est momentanément indisponible. Utilisez le paiement manuel ci-dessous.");
        }

        if (initResponse == null || initResponse.getData() == null
                || !"201".equals(initResponse.getCode()))
        {
            log.error("Échec de l'initialisation du paiement CinetPay pour la transaction {} : {}",
                    transactionId, initResponse != null ? initResponse.getMessage() : "réponse nulle");
            transaction.setStatus(PaymentTransactionStatusEnum.FAILED);
            paymentTransactionRepository.save(transaction);
            throw new PaymentProviderUnavailableException(
                    "Le paiement en ligne est momentanément indisponible. Utilisez le paiement manuel ci-dessous.");
        }

        transaction.setCinetpayPaymentToken(initResponse.getData().getPaymentToken());
        paymentTransactionRepository.save(transaction);

        return PaymentDTO.InitiatePaymentResponse.builder()
                .transactionId(transactionId)
                .paymentToken(initResponse.getData().getPaymentToken())
                .amount(planAmount)
                .currency(currency)
                .siteId(siteId)
                .build();
    }

    /**
     * Appelé par le contrôleur webhook. Idempotent et tolérant : ne lève
     * jamais d'exception vers CinetPay (qui réessaierait indéfiniment),
     * se contente de logger les cas anormaux.
     */
    @Transactional
    public void handleNotification(String transactionId)
    {
        if (transactionId == null || transactionId.isBlank())
        {
            log.warn("Notification CinetPay reçue sans transaction_id, ignorée");
            return;
        }

        PaymentTransactionEntity transaction = paymentTransactionRepository.findByTransactionId(transactionId)
                .orElse(null);
        if (transaction == null)
        {
            log.warn("Notification CinetPay pour une transaction inconnue : {}", transactionId);
            return;
        }

        if (transaction.getStatus() == PaymentTransactionStatusEnum.ACCEPTED)
        {
            log.info("Notification CinetPay dupliquée pour une transaction déjà confirmée : {}", transactionId);
            return;
        }

        PaymentDTO.CinetPayCheckResponse checkResponse = cinetPayClient.checkPayment(transactionId);
        if (checkResponse == null || checkResponse.getData() == null)
        {
            log.error("Impossible de vérifier la transaction {} auprès de CinetPay", transactionId);
            return;
        }

        String status = checkResponse.getData().getStatus();
        transaction.setCinetpayPaymentMethod(checkResponse.getData().getPaymentMethod());
        transaction.setCinetpayOperatorId(checkResponse.getData().getOperatorId());

        if (!"ACCEPTED".equalsIgnoreCase(status))
        {
            transaction.setStatus(PaymentTransactionStatusEnum.REFUSED);
            paymentTransactionRepository.save(transaction);
            log.info("Paiement refusé/non confirmé pour la transaction {} (statut CinetPay: {})",
                    transactionId, status);
            return;
        }

        transaction.setStatus(PaymentTransactionStatusEnum.ACCEPTED);
        transaction.setConfirmedAt(java.time.LocalDateTime.now());
        paymentTransactionRepository.save(transaction);

        UUID shopId = transaction.getShop().getId();
        boolean isFirstPayment = subscriptionService.confirmPayment(shopId);

        if (isFirstPayment)
        {
            // Déclenche la récompense du parrain, le cas échéant. La logique
            // de choix de boutique bénéficiaire (auto si une seule, sinon
            // en attente) vit dans ReferralService.
            referralService.onRefereeFirstPayment(shopId);
        }

        log.info("Paiement confirmé pour la boutique {} (transaction {})", shopId, transactionId);
    }
}
