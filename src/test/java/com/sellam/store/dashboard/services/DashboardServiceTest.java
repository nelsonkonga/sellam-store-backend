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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ProductsRepository productsRepository;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private ProductsService productsService;

    @InjectMocks
    private DashboardService dashboardService;

    private UUID shopId;

    @BeforeEach
    void setUp() {
        shopId = UUID.randomUUID();
    }

    @Test
    void testGetDashboardDataAggregatesCorrectly() {
        Object[] summaryRow = new Object[] {
                new BigDecimal("50000"), // totalSales
                new BigDecimal("15000"), // totalMargin
                5L,                      // invoiceCount
                new BigDecimal("10000")  // averageBasket
        };

        when(invoiceRepository.getSummaryReportData(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(summaryRow));
        when(productsRepository.countCriticalStock(shopId))
                .thenReturn(2L);

        InvoiceEntity invoice = InvoiceEntity.builder()
                .id(UUID.randomUUID())
                .invoiceNumber("FAC-001")
                .totalAmount(new BigDecimal("10000"))
                .build();
        when(invoiceRepository.findTodayInvoices(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class), eq(PageRequest.of(0, 10))))
                .thenReturn(List.of(invoice));

        InvoiceDTO.InvoiceResponse invoiceResponse = InvoiceDTO.InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber("FAC-001")
                .totalAmount(new BigDecimal("10000"))
                .build();
        when(invoiceService.toResponse(invoice)).thenReturn(invoiceResponse);

        ProductEntity product = ProductEntity.builder()
                .id(UUID.randomUUID())
                .name("Coca-Cola")
                .stockQuantity(new BigDecimal("2"))
                .alertThreshold(new BigDecimal("5"))
                .build();
        when(productsRepository.findCriticalStockProducts(eq(shopId), eq(PageRequest.of(0, 6))))
                .thenReturn(List.of(product));

        ProductsDTO.ProductResponse productResponse = ProductsDTO.ProductResponse.builder()
                .id(product.getId())
                .name("Coca-Cola")
                .stockQuantity(new BigDecimal("2"))
                .alertThreshold(new BigDecimal("5"))
                .build();
        when(productsService.toPostOutput(product)).thenReturn(productResponse);

        DashboardDTO.DashboardResponse response = dashboardService.getDashboardData(shopId);

        assertNotNull(response);
        assertNotNull(response.getKpis());
        assertEquals(new BigDecimal("50000"), response.getKpis().getTotalSalesToday());
        assertEquals(new BigDecimal("15000"), response.getKpis().getTotalMarginToday());
        assertEquals(5L, response.getKpis().getInvoiceCountToday());
        assertEquals(new BigDecimal("10000"), response.getKpis().getAverageBasket());
        assertEquals(2L, response.getKpis().getLowStockCount());

        assertEquals(1, response.getRecentInvoices().size());
        assertEquals("FAC-001", response.getRecentInvoices().get(0).getInvoiceNumber());

        assertEquals(1, response.getCriticalProducts().size());
        assertEquals("Coca-Cola", response.getCriticalProducts().get(0).getName());
    }

    @Test
    void testGetDashboardDataEmptyResults() {
        when(invoiceRepository.getSummaryReportData(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(productsRepository.countCriticalStock(shopId))
                .thenReturn(0L);
        when(invoiceRepository.findTodayInvoices(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class), eq(PageRequest.of(0, 10))))
                .thenReturn(Collections.emptyList());
        when(productsRepository.findCriticalStockProducts(eq(shopId), eq(PageRequest.of(0, 6))))
                .thenReturn(Collections.emptyList());

        DashboardDTO.DashboardResponse response = dashboardService.getDashboardData(shopId);

        assertNotNull(response);
        assertNotNull(response.getKpis());
        assertEquals(BigDecimal.ZERO, response.getKpis().getTotalSalesToday());
        assertEquals(BigDecimal.ZERO, response.getKpis().getTotalMarginToday());
        assertEquals(0L, response.getKpis().getInvoiceCountToday());
        assertEquals(BigDecimal.ZERO, response.getKpis().getAverageBasket());
        assertEquals(0L, response.getKpis().getLowStockCount());
        assertTrue(response.getRecentInvoices().isEmpty());
        assertTrue(response.getCriticalProducts().isEmpty());
    }
}
