package com.sellam.store.identity.dto;

import com.sellam.store.identity.models.LegacyEntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTOs pour la résolution des IDs legacy.
 */
public class LegacyMappingDTO
{
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ResolveResponse
    {
        private UUID legacyId;
        private UUID personId;
        private LegacyEntityType legacyType;
    }
}
