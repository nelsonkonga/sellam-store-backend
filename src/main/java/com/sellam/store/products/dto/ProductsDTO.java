package com.sellam.store.products.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class ProductsDTO {
    @Data
    @AllArgsConstructor
    @Builder
    public static class PostInput {

        @NonNull @NotBlank
        String name;

        String barcode;

        String pictureUrl;

        @NonNull
        UUID saleTypeId;

        @NonNull
        BigDecimal purchasePrice;

        @NonNull
        BigDecimal sellingPrice;

        @NonNull
        BigDecimal stockQuantity;

        @NonNull
        BigDecimal alertThreshold;

        @NonNull @NotBlank
        String category;

        LocalDate expirationDate;

        @NonNull
        UUID shopId;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class PostOutput {
        UUID id;

        String name;

        String barcode;

        String pictureUrl;

        UUID saleTypeId;
        String saleTypeName;
        String saleTypeUnitLabel;

        BigDecimal purchasePrice;

        BigDecimal sellingPrice;

        BigDecimal stockQuantity;

        BigDecimal alertThreshold;

        String category;

        LocalDateTime createdAt;

        LocalDate expirationDate;

        UUID shopId;
    }
}
