package com.sellam.store.payments.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

public class PaymentDTO
{
    /**
     * Réponse renvoyée au frontend après initiation : tout ce qu'il faut
     * pour ouvrir le widget CinetPay Seamless côté client.
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InitiatePaymentResponse
    {
        private String transactionId;
        private String paymentToken;
        private Integer amount;
        private String currency;
        private String siteId;
    }

    /**
     * Corps de la requête POST vers https://api-checkout.cinetpay.com/v2/payment
     * (champs documentés par CinetPay ; on n'expose que ceux réellement utilisés).
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CinetPayInitRequest
    {
        @JsonProperty("apikey")
        private String apikey;

        @JsonProperty("site_id")
        private String siteId;

        @JsonProperty("transaction_id")
        private String transactionId;

        @JsonProperty("amount")
        private Integer amount;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("description")
        private String description;

        @JsonProperty("notify_url")
        private String notifyUrl;

        @JsonProperty("return_url")
        private String returnUrl;

        @JsonProperty("channels")
        @Builder.Default
        private String channels = "ALL";

        @JsonProperty("customer_name")
        private String customerName;

        @JsonProperty("customer_email")
        private String customerEmail;

        @JsonProperty("customer_phone_number")
        private String customerPhoneNumber;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CinetPayInitResponse
    {
        private String code;
        private String message;
        private CinetPayInitData data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CinetPayInitData
    {
        @JsonProperty("payment_token")
        private String paymentToken;

        @JsonProperty("payment_url")
        private String paymentUrl;
    }

    /**
     * Réponse de l'API de vérification CinetPay
     * (POST https://api-checkout.cinetpay.com/v2/payment/check).
     * "code" == "00" et "data.status" == "ACCEPTED" signifient un paiement
     * confirmé — c'est la SEULE source de vérité, jamais le contenu brut
     * du webhook (cf. commentaire dans PaymentService).
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CinetPayCheckResponse
    {
        private String code;
        private String message;
        private CinetPayCheckData data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CinetPayCheckData
    {
        private String status; // ACCEPTED, REFUSED, etc.

        @JsonProperty("payment_method")
        private String paymentMethod;

        @JsonProperty("operator_id")
        private String operatorId;

        private Integer amount;
        private String currency;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InitiatePaymentRequest
    {
        private UUID shopId;
    }
}
