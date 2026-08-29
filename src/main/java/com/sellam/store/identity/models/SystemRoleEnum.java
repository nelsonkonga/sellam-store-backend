package com.sellam.store.identity.models;

/**
 * Rôles système au niveau plateforme, distincts des rôles métier par boutique.
 * Ces rôles s'appliquent à PersonEntity.systemRole et donnent des capacités
 * de niveau plateforme (ex: override des limites de changement de contact).
 */
public enum SystemRoleEnum
{
    /**
     * Administrateur plateforme avec capacités d'override.
     * Peut contourner les restrictions de changement de contact (3 mois).
     */
    PLATFORM_ADMIN
}
