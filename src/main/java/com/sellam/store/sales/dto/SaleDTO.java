package com.sellam.store.sales.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class SaleDTO
{

    @Data
    @AllArgsConstructor
    @Builder
    public static class SaleRequest
    {
        @NonNull
        private UUID productId;

        @NonNull
        private BigDecimal quantity;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class SaleResponse
    {
        private UUID id;

        private UUID productId;

        private String productName;

        private BigDecimal quantity;

        private BigDecimal totalPrice;

        private BigDecimal margin;

        private String status;

        private LocalDateTime soldAt;
    }
}