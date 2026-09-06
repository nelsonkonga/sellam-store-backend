package com.sellam.store.dashboard.dto;

import com.sellam.store.invoices.dto.InvoiceDTO;
import com.sellam.store.products.dto.ProductsDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class DashboardDTO {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class KpisResponse {
        private BigDecimal totalSalesToday;
        private BigDecimal totalMarginToday;
        private BigDecimal averageBasket;
        private long invoiceCountToday;
        private long lowStockCount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class DashboardResponse {
        private KpisResponse kpis;
        private List<InvoiceDTO.InvoiceResponse> recentInvoices;
        private List<ProductsDTO.ProductResponse> criticalProducts;
    }
}
