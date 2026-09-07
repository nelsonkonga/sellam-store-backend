package com.sellam.store.payments.exceptions;

/**
 * Levée quand CinetPay est injoignable ou refuse la requête (compte non
 * validé, clés invalides, service en panne...). Distincte d'une erreur
 * métier classique (boutique introuvable, etc.) pour que le contrôleur
 * puisse répondre avec un code HTTP dédié (503) que le frontend reconnaît
 * et utilise pour basculer automatiquement sur les instructions de
 * paiement manuel (cf. SubscriptionPage.jsx).
 */
public class PaymentProviderUnavailableException extends RuntimeException
{
    public PaymentProviderUnavailableException(String message)
    {
        super(message);
    }
}
