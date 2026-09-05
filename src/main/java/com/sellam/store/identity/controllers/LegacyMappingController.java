package com.sellam.store.identity.controllers;

import com.sellam.store.identity.dto.LegacyMappingDTO;
import com.sellam.store.identity.models.LegacyEntityType;
import com.sellam.store.identity.models.LegacyIdMapping;
import com.sellam.store.identity.repositories.LegacyIdMappingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Contrôleur pour la résolution des IDs legacy vers les nouveaux PersonEntity IDs.
 *
 * Permet la compatibilité avec le frontend qui utilise encore accountId/userId.
 *
 * Usage :
 * - GET /api/legacy/resolve/{legacyId} → Renvoie le personId correspondant
 * - Utilisé par les contrôleurs pour traduire les anciens IDs
 */
@RestController
@RequestMapping("/api/legacy")
public class LegacyMappingController
{

    private final LegacyIdMappingRepository legacyIdMappingRepository;

    public LegacyMappingController(LegacyIdMappingRepository legacyIdMappingRepository) {
        this.legacyIdMappingRepository = legacyIdMappingRepository;
    }

    /**
     * Résout un ID legacy (accountId ou userId) vers le PersonEntity.id correspondant.
     */
    @GetMapping("/resolve/{legacyId}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public LegacyMappingDTO.ResolveResponse resolveLegacyId(@PathVariable UUID legacyId)
    {
        LegacyIdMapping mapping = legacyIdMappingRepository.findByLegacyIdAndActiveTrue(legacyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ID legacy introuvable"));

        return LegacyMappingDTO.ResolveResponse.builder()
                .legacyId(mapping.getLegacyId())
                .personId(mapping.getPersonId())
                .legacyType(mapping.getLegacyType())
                .build();
    }

    /**
     * Résout un ID legacy avec type explicite (pour cas ambigus).
     */
    @GetMapping("/resolve/{legacyId}/{type}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public LegacyMappingDTO.ResolveResponse resolveLegacyIdWithType(
            @PathVariable UUID legacyId,
            @PathVariable String type)
    {
        LegacyEntityType legacyType;
        try
        {
            legacyType = LegacyEntityType.valueOf(type.toUpperCase());
        }
        catch (IllegalArgumentException e)
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type invalide : ACCOUNT ou USER attendu");
        }

        LegacyIdMapping mapping = legacyIdMappingRepository.findByLegacyIdAndLegacyType(legacyId, legacyType)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ID legacy introuvable"));

        return LegacyMappingDTO.ResolveResponse.builder()
                .legacyId(mapping.getLegacyId())
                .personId(mapping.getPersonId())
                .legacyType(mapping.getLegacyType())
                .build();
    }
}
