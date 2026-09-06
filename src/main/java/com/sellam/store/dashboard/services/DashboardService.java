package com.sellam.store.dashboard.services;

import com.sellam.store.dashboard.dto.DashboardDTO;
import com.sellam.store.invoices.dto.InvoiceDTO;
import com.sellam.store.invoices.models.InvoiceEntity;
import com.sellam.store.invoices.repositories.InvoiceRepository;
import com.sellam.store.invoices.services.InvoiceService;
import com.sellam.store.products.dto.ProductsDTO;
import com.sellam.store.products.models.ProductEntity;
import com.sellam.store.products.repositories.ProductsRepository;
import com.sellam.store.products.services.ProductsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final InvoiceRepository invoiceRepository;
    private final ProductsRepository productsRepository;
    private final InvoiceService invoiceService;
    private final ProductsService productsService;

    @Transactional(readOnly = true)
    public DashboardDTO.DashboardResponse getDashboardData(UUID shopId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        // 1. KPIs via InvoiceRepository.getSummaryReportData
        List<Object[]> summary = invoiceRepository.getSummaryReportData(shopId, startOfDay, endOfDay);
        BigDecimal totalSalesToday = BigDecimal.ZERO;
        BigDecimal totalMarginToday = BigDecimal.ZERO;
        long invoiceCountToday = 0L;
        BigDecimal averageBasket = BigDecimal.ZERO;

        if (summary != null && !summary.isEmpty()) {
            Object[] row = summary.get(0);
            if (row[0] != null) totalSalesToday = new BigDecimal(row[0].toString());
            if (row[1] != null) totalMarginToday = new BigDecimal(row[1].toString());
            if (row[2] != null) invoiceCountToday = ((Number) row[2]).longValue();
            if (row[3] != null) averageBasket = new BigDecimal(row[3].toString());
        }

        // 2. Low stock count
        long lowStockCount = productsRepository.countCriticalStock(shopId);

        DashboardDTO.KpisResponse kpis = DashboardDTO.KpisResponse.builder()
                .totalSalesToday(totalSalesToday)
                .totalMarginToday(totalMarginToday)
                .averageBasket(averageBasket)
                .invoiceCountToday(invoiceCountToday)
                .lowStockCount(lowStockCount)
                .build();

        // 3. Recent invoices (top 10 today)
        List<InvoiceEntity> recentInvoicesEntities = invoiceRepository.findTodayInvoices(shopId, startOfDay, endOfDay, PageRequest.of(0, 10));
        List<InvoiceDTO.InvoiceResponse> recentInvoices = recentInvoicesEntities.stream()
                .map(invoiceService::toResponse)
                .collect(Collectors.toList());

        // 4. Critical products (top 6 low stock)
        List<ProductEntity> criticalProductsEntities = productsRepository.findCriticalStockProducts(shopId, PageRequest.of(0, 6));
        List<ProductsDTO.ProductResponse> criticalProducts = criticalProductsEntities.stream()
                .map(productsService::toPostOutput)
                .collect(Collectors.toList());

        return DashboardDTO.DashboardResponse.builder()
                .kpis(kpis)
                .recentInvoices(recentInvoices)
                .criticalProducts(criticalProducts)
                .build();
    }
}
