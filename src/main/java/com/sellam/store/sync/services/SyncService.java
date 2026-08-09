package com.sellam.store.sync.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

    public SyncService(InvoiceService invoiceService)
    {
        this.invoiceService = invoiceService;
    }

    @Transactional
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
                processed.add(action.getLocalId());
            }
            catch (IllegalArgumentException e)
            {
                // Conflit métier (stock insuffisant, données invalides)
                conflicts.add("Action " + action.getLocalId() + " : " + e.getMessage());
            }
            catch (Exception e)
            {
                // Erreur technique inattendue — loggée en plus d'être remontée au client
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

    private void processRegisterSale(SyncDTO.PendingAction action)
    {
        UUID productId = UUID.fromString((String) action.getPayload().get("productId"));
        BigDecimal quantity = new BigDecimal(action.getPayload().get("quantity").toString());
        UUID shopId = UUID.fromString(action.getShopId());

        // 1. Créer une facture pour cette vente offline
        InvoiceDTO.CreateInvoiceRequest createReq = InvoiceDTO.CreateInvoiceRequest.builder()
                .customerName(null)
                .build();
        InvoiceDTO.InvoiceResponse invoice = invoiceService.createInvoice(shopId, createReq);

        // 2. Ajouter la ligne de vente
        InvoiceDTO.AddLineRequest lineReq = InvoiceDTO.AddLineRequest.builder()
                .productId(productId)
                .quantity(quantity)
                .build();
        invoiceService.addLine(invoice.getId(), lineReq);

        // 3. Valider la facture
        invoiceService.validateInvoice(invoice.getId());
    }
}