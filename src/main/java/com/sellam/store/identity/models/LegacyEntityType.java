package com.sellam.store.identity.models;

/**
 * Types d'entités héritées pour le mapping des IDs pendant la migration.
 */
public enum LegacyEntityType
{
    /**
     * Ancien AccountEntity (gérant propriétaire).
     */
    ACCOUNT,
    
    /**
     * Ancien UserEntity (employé).
     */
    USER
}
