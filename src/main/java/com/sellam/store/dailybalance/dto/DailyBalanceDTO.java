package com.sellam.store.dailybalance.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class DailyBalanceDTO
{

    @Data
    @AllArgsConstructor
    @Builder
    public static class DeclareCashRequest
    {
        @NonNull
        private BigDecimal declaredCash;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class DailyBalanceResponse
    {
        private UUID id;
        private LocalDate balanceDate;
        private BigDecimal computedTotalSales;
        private BigDecimal computedTotalMargin;
        private BigDecimal declaredCash;
        private BigDecimal discrepancy;
        private String status;
        private LocalDateTime createdAt;
    }
}