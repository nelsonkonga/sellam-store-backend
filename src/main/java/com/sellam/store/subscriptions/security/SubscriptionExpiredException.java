package com.sellam.store.subscriptions.security;

public class SubscriptionExpiredException extends RuntimeException
{
    public SubscriptionExpiredException()
    {
        super("Votre abonnement a expiré. Renouvelez-le pour continuer à utiliser Sellam.");
    }
}
