package com.sellam.store.invoices.services;

import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.invoices.dto.InvoiceDTO;
import com.sellam.store.invoices.models.DiscountTypeEnum;
import com.sellam.store.invoices.models.InvoiceEntity;
import com.sellam.store.invoices.models.InvoiceStatusEnum;
import com.sellam.store.invoices.repositories.InvoiceCounterRepository;
import com.sellam.store.invoices.repositories.InvoiceHistoryRepository;
import com.sellam.store.invoices.repositories.InvoiceRepository;
import com.sellam.store.products.repositories.ProductsRepository;
import com.sellam.store.sales.repositories.SalesRepository;
import com.sellam.store.shops.repositories.ShopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTotalsTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceCounterRepository counterRepository;
    @Mock private SalesRepository salesRepository;
    @Mock private ProductsRepository productsRepository;
    @Mock private ShopRepository shopRepository;
    @Mock private PersonRepository personRepository;
    @Mock private InvoiceHistoryRepository invoiceHistoryRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private UUID invoiceId;
    private InvoiceEntity invoice;

    @BeforeEach
    void setUp() {
        invoiceId = UUID.randomUUID();
        invoice = InvoiceEntity.builder()
                .id(invoiceId)
                .status(InvoiceStatusEnum.OPEN)
                .subtotal(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .totalMargin(BigDecimal.ZERO)
                .build();
    }

    @Test
    void testApplyInvoiceDiscountUsesSqlAggregatedTotals() {
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(salesRepository.computeInvoiceTotals(invoiceId)).thenReturn(
                Collections.singletonList(new Object[] {
                        BigDecimal.valueOf(10000), // subtotal
                        BigDecimal.valueOf(3000)   // marginSum
                })
        );
        when(salesRepository.findByInvoice_Id(invoiceId)).thenReturn(Collections.emptyList());

        InvoiceDTO.ApplyInvoiceDiscountRequest req = InvoiceDTO.ApplyInvoiceDiscountRequest.builder()
                .discountType("PERCENTAGE")
                .discountValue(BigDecimal.valueOf(10)) // 10% de 10000 = 1000
                .build();

        InvoiceDTO.InvoiceResponse res = invoiceService.applyInvoiceDiscount(invoiceId, req);

        assertNotNull(res);
        verify(salesRepository).computeInvoiceTotals(invoiceId);
        assertEquals(BigDecimal.valueOf(10000), invoice.getSubtotal());
        assertEquals(new BigDecimal("1000.00"), invoice.getDiscountAmount());
        assertEquals(new BigDecimal("9000.00"), invoice.getTotalAmount());
        assertEquals(new BigDecimal("2000.00"), invoice.getTotalMargin());
    }
}
