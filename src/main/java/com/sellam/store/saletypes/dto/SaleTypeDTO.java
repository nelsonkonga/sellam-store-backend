package com.sellam.store.saletypes.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.UUID;

public class SaleTypeDTO {

    @Data
    @AllArgsConstructor
    @Builder
    public static class Request {
        @NonNull @NotBlank
        private String name;

        @NonNull @NotBlank
        private String unitLabel;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class Response {
        private UUID id;
        private String name;
        private String unitLabel;
        private boolean isDefault;
        private UUID shopId;
    }
}
