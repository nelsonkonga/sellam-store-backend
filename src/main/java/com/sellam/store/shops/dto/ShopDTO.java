package com.sellam.store.shops.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

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
        Boolean autoPrintInvoices;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ShopSettingsRequest
    {
        private Boolean autoPrintInvoices;
    }
}
