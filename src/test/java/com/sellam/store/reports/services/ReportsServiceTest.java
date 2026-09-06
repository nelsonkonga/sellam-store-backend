package com.sellam.store.reports.services;

import com.sellam.store.cash.repositories.CashRegisterSessionRepository;
import com.sellam.store.invoices.repositories.InvoiceRepository;
import com.sellam.store.products.repositories.ProductsRepository;
import com.sellam.store.reports.dto.ReportsDTO;
import com.sellam.store.sales.repositories.SalesRepository;
import com.sellam.store.shops.repositories.ShopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportsServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private SalesRepository salesRepository;

    @Mock
    private CashRegisterSessionRepository cashSessionRepository;

    @Mock
    private ProductsRepository productsRepository;

    @Mock
    private ShopRepository shopRepository;

    @InjectMocks
    private ReportsService reportsService;

    private UUID shopId;

    @BeforeEach
    void setUp() {
        shopId = UUID.randomUUID();
    }

    @Test
    void testGetSummaryReportUsesDatabaseAggregation() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 6);

        Object[] summaryRow = new Object[] {
                BigDecimal.valueOf(150000), // totalRevenue
                BigDecimal.valueOf(45000),  // totalMargin
                10L,                        // count
                BigDecimal.valueOf(15000)   // avgBasket
        };

        when(invoiceRepository.getSummaryReportData(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(java.util.Collections.singletonList(summaryRow));

        ReportsDTO.SummaryReport report = reportsService.getSummaryReport(shopId, start, end);

        assertNotNull(report);
        assertEquals(BigDecimal.valueOf(150000), report.getTotalRevenue());
        assertEquals(BigDecimal.valueOf(45000), report.getTotalMargin());
        assertEquals(10, report.getInvoiceCount());
        assertEquals(BigDecimal.valueOf(15000), report.getAverageBasket());
    }

    @Test
    void testGetCashReliabilityUsesDatabaseAggregation() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 6);

        List<Object[]> rows = List.of(
                new Object[] { LocalDate.of(2026, 9, 2), BigDecimal.valueOf(-500) },
                new Object[] { LocalDate.of(2026, 9, 3), BigDecimal.valueOf(0) },
                new Object[] { LocalDate.of(2026, 9, 4), BigDecimal.valueOf(250) }
        );

        when(cashSessionRepository.getDailyDiscrepancies(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(rows);

        List<ReportsDTO.CashReliabilityItem> items = reportsService.getCashReliability(shopId, start, end);

        assertNotNull(items);
        assertEquals(3, items.size());
        assertEquals(LocalDate.of(2026, 9, 2), items.get(0).getDate());
        assertEquals(BigDecimal.valueOf(-500), items.get(0).getDiscrepancy());
        assertEquals(BigDecimal.valueOf(250), items.get(2).getDiscrepancy());
    }

    @Test
    void testGetDormantStockValueUsesDatabaseAggregation() {
        Object[] summaryRow = new Object[] {
                8L,                        // count
                BigDecimal.valueOf(240000) // totalValue
        };

        when(productsRepository.getDormantStockSummary(eq(shopId), any(LocalDateTime.class)))
                .thenReturn(java.util.Collections.singletonList(summaryRow));

        ReportsDTO.DormantStockValue dormant = reportsService.getDormantStockValue(shopId);

        assertNotNull(dormant);
        assertEquals(8, dormant.getDormantProductCount());
        assertEquals(BigDecimal.valueOf(240000), dormant.getTotalDormantValue());
    }
}
