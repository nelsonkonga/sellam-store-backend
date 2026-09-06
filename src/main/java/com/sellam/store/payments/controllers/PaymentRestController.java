package com.sellam.store.payments.controllers;

import com.sellam.store.common.security.IShopAccessGuard;
import com.sellam.store.payments.dto.PaymentDTO;
import com.sellam.store.payments.services.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@AllArgsConstructor
public class PaymentRestController
{
    private final PaymentService paymentService;
    private final IShopAccessGuard shopAccessGuard;

    @PostMapping("/api/shops/{shopId}/subscription/payment/initiate")
    @ResponseStatus(HttpStatus.OK)
    public PaymentDTO.InitiatePaymentResponse initiatePayment(
            @PathVariable UUID shopId,
            Authentication authentication)
    {
        shopAccessGuard.requireShopAccess(authentication, shopId);
        return paymentService.initiatePayment(shopId);
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
