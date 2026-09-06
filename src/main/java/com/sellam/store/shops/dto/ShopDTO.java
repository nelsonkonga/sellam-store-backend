package com.sellam.store.shops.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.UUID;

public class ShopDTO
{
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ShopRequest
    {
        @NonNull
        String name;

        @NonNull
        String address;

        String logoUrl;
        @Pattern(regexp = "^(|\\+[1-9]\\d{6,14})$", message = "Le numéro doit être vide ou au format E.164 (ex: +237690000000)")
        String phoneNumber;
        String taxpayerNumber;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ShopResponse
    {
        UUID id;
        String name;
        String address;
        String logoUrl;
        String phoneNumber;
        String taxpayerNumber;
        Boolean autoPrintInvoices;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ShopSettingsRequest
    {
        private Boolean autoPrintInvoices;
        private String logoUrl; // patch partiel : null = inchangé (voir ShopService.updateSettings)
        @Pattern(regexp = "^(|\\+[1-9]\\d{6,14})$", message = "Le numéro doit être vide ou au format E.164 (ex: +237690000000)")
        private String phoneNumber;
        private String taxpayerNumber;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ShopSummaryResponse
    {
        private UUID id;
        private String name;
        private String address;
        private String logoUrl;
        private String phoneNumber;
        private String taxpayerNumber;
        private Boolean autoPrintInvoices;
        private BigDecimal salesToday;
        private String margin;
        private long teamCount;
    }
}