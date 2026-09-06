package com.sellam.store.payments.services;

import com.sellam.store.payments.dto.PaymentDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Client bas niveau pour l'API CinetPay Checkout v2.
 * Documentation : https://docs.cinetpay.com/api/1.0-fr/checkout/initialisation
 *
 * En sandbox, base-url reste la même que la prod (cinetpay.base-url) :
 * c'est l'API key/site_id du compte marchand qui détermine le mode
 * sandbox vs réel, pas l'URL. Vérifiez dans votre dashboard CinetPay que
 * les identifiants utilisés ici sont bien ceux du mode "Test".
 */
@Slf4j
@Component
public class CinetPayClient
{
    private final RestClient restClient;

    @Value("${cinetpay.api-key:${payment-integration.api-key:dummy-api-key}}")
    private String apiKey;

    @Value("${cinetpay.site-id:${payment-integration.site-id:dummy-site-id}}")
    private String siteId;

    @Value("${cinetpay.secret-key:${payment-integration.secret-key:dummy-secret-key}}")
    private String secretKey;

    @Value("${cinetpay.notify-url:${payment-integration.notify-url:http://localhost:8080/notify}}")
    private String notifyUrl;

    @Value("${cinetpay.return-url:${payment-integration.return-url:http://localhost:8080/return}}")
    private String returnUrl;

    @Value("${cinetpay.currency:${payment-integration.currency:XOF}}")
    private String currency;

    public CinetPayClient(@Value("${cinetpay.base-url:${payment-integration.base-url:https://api.cinetpay.net}}") String baseUrl)
    {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public PaymentDTO.CinetPayInitResponse initPayment(
            String transactionId, int amount, String description,
            String customerName, String customerEmail, String customerPhoneNumber)
    {
        PaymentDTO.CinetPayInitRequest request = PaymentDTO.CinetPayInitRequest.builder()
                .apikey(apiKey)
                .siteId(siteId)
                .transactionId(transactionId)
                .amount(amount)
                .currency(currency)
                .description(description)
                .notifyUrl(notifyUrl)
                .returnUrl(returnUrl)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .customerPhoneNumber(customerPhoneNumber)
                .build();

        return restClient.post()
                .uri("/payment")
                .body(request)
                .retrieve()
                .body(PaymentDTO.CinetPayInitResponse.class);
    }

    /**
     * Revérifie l'état réel d'une transaction directement auprès de
     * CinetPay. C'est la SEULE méthode dont le résultat doit être utilisé
     * pour confirmer un paiement — jamais le contenu du POST de
     * notification lui-même, qui n'est pas authentifié et peut être
     * forgé par un tiers connaissant/ devinant l'URL du webhook.
     */
    public PaymentDTO.CinetPayCheckResponse checkPayment(String transactionId)
    {
        Map<String, String> body = Map.of(
                "apikey", apiKey,
                "site_id", siteId,
                "transaction_id", transactionId
        );

        return restClient.post()
                .uri("/payment/check")
                .body(body)
                .retrieve()
                .body(PaymentDTO.CinetPayCheckResponse.class);
    }
}
