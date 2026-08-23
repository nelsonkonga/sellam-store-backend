package com.sellam.store.sync.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sellam.store.invoices.dto.InvoiceDTO;
import com.sellam.store.invoices.services.InvoiceService;
import com.sellam.store.sync.dto.SyncDTO;

@Service
public class SyncService
{

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final InvoiceService invoiceService;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    public SyncService(InvoiceService invoiceService, org.springframework.transaction.support.TransactionTemplate transactionTemplate)
    {
        this.invoiceService = invoiceService;
        this.transactionTemplate = transactionTemplate;
    }

    public SyncDTO.SyncResponse processSync(SyncDTO.SyncRequest request)
    {
        List<Long> processed = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();

        if (request == null || request.getActions() == null) {
            return SyncDTO.SyncResponse.builder()
                    .processedActionIds(processed)
                    .conflicts(conflicts)
                    .build();
        }

        for (SyncDTO.PendingAction action : request.getActions())
        {
            try
            {
                if ("REGISTER_SALE".equals(action.getType()))
                {
                    processRegisterSale(action);
                }
                else if ("SYNC_INVOICE".equals(action.getType()))
                {
                    processSyncInvoice(action);
                }
                processed.add(action.getLocalId());
            }
            catch (IllegalArgumentException e)
            {
                conflicts.add("Action " + action.getLocalId() + " : " + e.getMessage());
            }
            catch (Exception e)
            {
                log.error("[SYNC] Erreur technique inattendue pour action localId={} type={}: {}",
                        action.getLocalId(), action.getType(), e.getMessage(), e);
                conflicts.add("Action " + action.getLocalId() + " : erreur technique — " + e.getMessage());
            }
        }

        return SyncDTO.SyncResponse.builder()
                .processedActionIds(processed)
                .conflicts(conflicts)
                .build();
    }

    @SuppressWarnings("unchecked")
    private void processSyncInvoice(SyncDTO.PendingAction action)
    {
        transactionTemplate.execute(status -> {
            UUID shopId = UUID.fromString(action.getShopId());
            Map<String, Object> payload = action.getPayload();
            
            String customerName = (String) payload.get("customerName");
            
            // 1. Create invoice
            InvoiceDTO.CreateInvoiceRequest createReq = InvoiceDTO.CreateInvoiceRequest.builder()
                    .customerName(customerName)
                    .build();
            InvoiceDTO.InvoiceResponse invoice = invoiceService.createInvoice(shopId, createReq);
            
            // 2. Add lines
            List<Map<String, Object>> lines = (List<Map<String, Object>>) payload.get("lines");
            if (lines != null) {
                for (Map<String, Object> line : lines) {
                    UUID productId = UUID.fromString((String) line.get("productId"));
                    BigDecimal quantity = new BigDecimal(line.get("quantity").toString());
                    
                    InvoiceDTO.AddLineRequest.AddLineRequestBuilder lineReq = InvoiceDTO.AddLineRequest.builder()
                            .productId(productId)
                            .quantity(quantity);
                            
                    // Optional line discount
                    if (line.get("lineDiscountAmount") != null) {
                        lineReq.discountValue(new BigDecimal(line.get("lineDiscountAmount").toString()));
                    }
                    if (line.get("lineDiscountType") != null) {
                        lineReq.discountType((String) line.get("lineDiscountType"));
                    }
                    
                    invoiceService.addLine(invoice.getId(), lineReq.build());
                }
            }
            
            // 3. Apply global discount if any
            if (payload.get("discountAmount") != null) {
                BigDecimal discountAmt = new BigDecimal(payload.get("discountAmount").toString());
                String discountType = (String) payload.get("discountType");
                
                InvoiceDTO.ApplyInvoiceDiscountRequest discountReq = InvoiceDTO.ApplyInvoiceDiscountRequest.builder()
                        .discountValue(discountAmt)
                        .discountType(discountType)
                        .build();
                invoiceService.applyInvoiceDiscount(invoice.getId(), discountReq);
            }
            
            String paymentMethod = (String) payload.get("paymentMethod");
            
            // 4. Validate invoice
            InvoiceDTO.ValidateInvoiceRequest validateReq = InvoiceDTO.ValidateInvoiceRequest.builder()
                    .customerName(customerName)
                    .paymentMethod(paymentMethod)
                    .build();
            invoiceService.validateInvoice(invoice.getId(), validateReq, null);
            return null;
        });
    }

    private void processRegisterSale(SyncDTO.PendingAction action)
    {
        transactionTemplate.execute(status -> {
            UUID productId = UUID.fromString((String) action.getPayload().get("productId"));
            BigDecimal quantity = new BigDecimal(action.getPayload().get("quantity").toString());
            UUID shopId = UUID.fromString(action.getShopId());

            InvoiceDTO.CreateInvoiceRequest createReq = InvoiceDTO.CreateInvoiceRequest.builder()
                    .customerName(null)
                    .build();
            InvoiceDTO.InvoiceResponse invoice = invoiceService.createInvoice(shopId, createReq);

            InvoiceDTO.AddLineRequest lineReq = InvoiceDTO.AddLineRequest.builder()
                    .productId(productId)
                    .quantity(quantity)
                    .build();
            invoiceService.addLine(invoice.getId(), lineReq);

            invoiceService.validateInvoice(invoice.getId(), null, null);
            return null;
        });
    }
}