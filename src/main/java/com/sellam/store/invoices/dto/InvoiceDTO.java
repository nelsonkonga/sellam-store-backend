package com.sellam.store.invoices.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class InvoiceDTO
{

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CreateInvoiceRequest
    {
        private String customerName;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class AddLineRequest
    {
        @NonNull private UUID productId;
        @NonNull private BigDecimal quantity;
        private String discountType;
        private BigDecimal discountValue;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ApplyInvoiceDiscountRequest
    {
        @NonNull private String discountType;
        @NonNull private BigDecimal discountValue;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class LineResponse {
        private UUID saleId;
        private String productName;
        private BigDecimal quantity;
        private BigDecimal lineSubtotal;
        private BigDecimal discountAmount;
        private BigDecimal totalPrice;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class InvoiceResponse
    {
        private UUID id;
        private String invoiceNumber;
        private String customerName;
        private List<LineResponse> lines;
        private BigDecimal subtotal;
        private BigDecimal discountAmount;
        private BigDecimal totalAmount;
        private String status;
        private LocalDateTime createdAt;
    }
}