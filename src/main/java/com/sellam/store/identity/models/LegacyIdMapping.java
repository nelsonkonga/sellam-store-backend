package com.sellam.store.identity.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Table de correspondance temporaire pour la compatibilité des IDs pendant la migration.
 * 
 * Permet de mapper les anciens IDs (AccountEntity.id, UserEntity.id) vers les nouveaux
 * PersonEntity.id, assurant la compatibilité avec le frontend qui utilise encore accountId.
 * 
 * Cette table peut être supprimée après la complète transition du frontend vers personId.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="legacy_id_mappings",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "legacy_id", name = "uk_legacy_id")
       })
@Entity
public class LegacyIdMapping
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Ancien ID (AccountEntity.id ou UserEntity.id) utilisé par le frontend.
     */
    @Column(name = "legacy_id", unique = true, nullable = false)
    private UUID legacyId;

    /**
     * Nouveau PersonEntity.id correspondant.
     */
    @Column(name = "person_id", nullable = false)
    private UUID personId;

    /**
     * Type de l'ancienne entité : "ACCOUNT" ou "USER".
     * Permet de distinguer la source du mapping.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "legacy_type", nullable = false)
    private LegacyEntityType legacyType;

    /**
     * Date de création du mapping (pour audit et nettoyage éventuel).
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Indique si ce mapping est encore actif.
     * Permet une désactivation progressive sans suppression immédiate.
     */
    @Builder.Default
    private boolean active = true;
}
