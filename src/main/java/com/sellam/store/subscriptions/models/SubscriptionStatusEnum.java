package com.sellam.store.subscriptions.models;

/**
 * Statut de l'abonnement d'une boutique.
 *
 * TRIAL      -> essai gratuit en cours
 * TRIAL_ENDING -> essai gratuit en cours, mais proche de l'expiration
 *                 (déclenche les avertissements, façon Shopify)
 * ACTIVE     -> abonnement payant actif
 * PAST_DUE   -> abonnement payant expiré / paiement non renouvelé,
 *               période de grâce courte avant blocage effectif
 * EXPIRED    -> plus aucun accès (essai ou abonnement expiré, période de grâce passée)
 */
public enum SubscriptionStatusEnum
{
    TRIAL,
    TRIAL_ENDING,
    ACTIVE,
    PAST_DUE,
    EXPIRED
}
