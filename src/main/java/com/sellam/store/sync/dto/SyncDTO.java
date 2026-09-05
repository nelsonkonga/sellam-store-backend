package com.sellam.store.sync.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class SyncDTO
{

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PendingAction
    {
        private Long localId;
        private String type;
        private String shopId;
        private Map<String, Object> payload;
        private String createdAt;
        private String localCreatedAt; // Date réelle de création locale pour factures
        private String soldBy; // ID de l'utilisateur qui a créé la facture
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SyncRequest
    {
        private List<PendingAction> actions;
    }

    @Data
    @Builder
    public static class SyncResponse
    {
        private List<Long> processedActionIds;
        private List<String> conflicts;
    }
}