package com.sellam.store.payments.controllers;

import com.sellam.store.common.security.IShopAccessGuard;
import com.sellam.store.payments.dto.PaymentDTO;
import com.sellam.store.payments.services.PaymentService;
import com.sellam.store.payments.dto.ManualPaymentInfoResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
public class PaymentRestController
{
    private final PaymentService paymentService;
    private final IShopAccessGuard shopAccessGuard;

    @Value("${app.manual-payment.mobile-money-number}")
    private String manualPaymentNumber;

    @Value("${app.manual-payment.mobile-money-holder-name}")
    private String manualPaymentHolderName;

    @Value("${app.manual-payment.mobile-money-operator}")
    private String manualPaymentOperator;

    @Value("${app.manual-payment.support-contact-url:/support/tickets/new}")
    private String manualPaymentSupportUrl;

    @Value("${app.subscription.plan-amount-xaf}")
    private int planAmountXaf;

    // Constructeur explicite pour injecter uniquement les services et ignorer les @Value
    public PaymentRestController(PaymentService paymentService, IShopAccessGuard shopAccessGuard) {
        this.paymentService = paymentService;
        this.shopAccessGuard = shopAccessGuard;
    }

    @PostMapping("/api/shops/{shopId}/subscription/payment/initiate")
    @ResponseStatus(HttpStatus.OK)
    public PaymentDTO.InitiatePaymentResponse initiatePayment(
            @PathVariable UUID shopId,
            Authentication authentication)
    {
        shopAccessGuard.requireShopAccess(authentication, shopId);
        return paymentService.initiatePayment(shopId);
    }

    @GetMapping("/api/payments/manual-payment-info")
    @ResponseStatus(HttpStatus.OK)
    public ManualPaymentInfoResponse getManualPaymentInfo()
    {
        return ManualPaymentInfoResponse.builder()
                .mobileMoneyNumber(manualPaymentNumber)
                .mobileMoneyHolderName(manualPaymentHolderName)
                .mobileMoneyOperator(manualPaymentOperator)
                .supportContactUrl(manualPaymentSupportUrl)
                .planAmountXaf(planAmountXaf)
                .build();
    }

    /**
     * Webhook server-to-server appelé par CinetPay (notify_url). Ne doit
     * PAS être protégé par le JwtAuthFilter classique : CinetPay n'a pas
     * de JWT applicatif. Cette route doit être ajoutée aux exceptions
     * publiques de SecurityConfig (cf. PATCH_SecurityConfig), tout en
     * restant sûre puisque handleNotification revérifie tout auprès de
     * CinetPay avant d'agir (cf. commentaires dans PaymentService).
     *
     * CinetPay envoie le corps en x-www-form-urlencoded, pas en JSON :
     * on lit directement le paramètre de requête plutôt qu'un DTO typé.
     */
    @PostMapping(value = "/api/payments/cinetpay/notify",
            consumes = {"application/x-www-form-urlencoded", "application/json"})
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> handleCinetPayNotification(HttpServletRequest request)
    {
        String transactionId = request.getParameter("cpm_trans_id");
        if (transactionId == null)
        {
            // Certaines configurations CinetPay envoient "transaction_id"
            // au lieu de "cpm_trans_id" selon la version d'intégration.
            transactionId = request.getParameter("transaction_id");
        }

        log.info("Notification CinetPay reçue pour transaction_id={}", transactionId);
        paymentService.handleNotification(transactionId);

        // CinetPay attend une réponse HTTP 200 simple pour considérer la
        // notification comme reçue ; le contenu exact importe peu.
        return Map.of("status", "ok");
    }
}
