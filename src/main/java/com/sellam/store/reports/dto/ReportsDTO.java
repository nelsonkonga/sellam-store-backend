package com.sellam.store.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ReportsDTO {

    @Data
    @Builder
    public static class SummaryReport {
        private BigDecimal totalRevenue;
        private BigDecimal totalMargin;
        private int invoiceCount;
        private BigDecimal averageBasket;
    }

    @Data
    @Builder
    public static class TopProductItem {
        private String productName;
        private BigDecimal quantitySold;
        private BigDecimal marginGenerated;
    }

    @Data
    @Builder
    public static class CashReliabilityItem {
        private LocalDate date;
        private BigDecimal discrepancy;
    }

    @Data
    @Builder
    public static class NegativeMarginSaleItem {
        private UUID saleId;
        private String productName;
        private BigDecimal quantity;
        private BigDecimal unitSellingPrice;
        private BigDecimal unitPurchasePrice;
        private BigDecimal margin;
        private String date;
    }

    // --- VAGUE 2 ---
    @Data
    @Builder
    public static class CategoryPerformanceItem {
        private String category;
        private BigDecimal revenue;
        private BigDecimal margin;
        private BigDecimal quantitySold;
    }

    @Data
    @Builder
    public static class DeadStockItem {
        private String productName;
        private BigDecimal stockQuantity;
        private Integer daysSinceLastSale;
    }

    @Data
    @Builder
    public static class PaymentMethodItem {
        private String method;
        private BigDecimal totalRevenue;
        private Integer invoiceCount;
    }

    @Data
    @Builder
    public static class EmployeePerformanceItem {
        private String employeeName;
        private BigDecimal totalRevenue;
        private Integer invoiceCount;
    }

    @Data
    @Builder
    public static class CustomerPerformanceItem {
        private String customerName;
        private BigDecimal totalRevenue;
        private Integer invoiceCount;
    }

    // --- VAGUE 3 ---
    @Data
    @Builder
    public static class CashflowProjection {
        private BigDecimal past30DaysRevenue;
        private BigDecimal projectedNext30DaysRevenue;
        private BigDecimal averageDailyRevenue;
    }

    @Data
    @Builder
    public static class DormantStockValue {
        private BigDecimal totalDormantValue;
        private Integer dormantProductCount;
    }

    @Data
    @Builder
    public static class StockOutAlertItem {
        private String productName;
        private BigDecimal currentStock;
        private BigDecimal dailyVelocity;
        private Integer estimatedDaysRemaining;
    }

    @Data
    @Builder
    public static class FrequentlyBoughtTogetherItem {
        private String productA;
        private String productB;
        private Integer pairOccurrences;
    }

    @Data
    @Builder
    public static class SalesAnomalyItem {
        private String date;
        private BigDecimal revenue;
        private BigDecimal averageRevenue;
        private String anomalyType; // "PIC" ou "CREUX"
    }
}
