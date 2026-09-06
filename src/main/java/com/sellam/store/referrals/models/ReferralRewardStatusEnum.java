package com.sellam.store.referrals.models;

/**
 * PENDING  : le filleul a payé, la récompense existe mais attend que le
 *            parrain choisisse la boutique bénéficiaire (cas multi-boutique).
 * APPLIED  : jours bonus déjà crédités sur une boutique précise.
 * EXPIRED  : non utilisée pour l'instant, réservée si on décide plus tard
 *            d'imposer un délai pour choisir (garder l'enum ouvert).
 */
public enum ReferralRewardStatusEnum
{
    PENDING,
    APPLIED,
    EXPIRED
}
