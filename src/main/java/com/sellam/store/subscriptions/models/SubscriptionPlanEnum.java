package com.sellam.store.subscriptions.models;

/**
 * Un seul plan payant pour l'instant (cf. discussion produit).
 * Le prix et la durée de l'essai gratuit restent configurables via
 * application.properties pour ne pas coder les valeurs en dur.
 */
public enum SubscriptionPlanEnum
{
    FREE_TRIAL,
    STANDARD
}
