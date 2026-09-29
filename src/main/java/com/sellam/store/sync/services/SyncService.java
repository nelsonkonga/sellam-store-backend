package com.sellam.store.sync.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sellam.store.common.security.AuthPrincipal;
import com.sellam.store.invoices.dto.InvoiceDTO;
import com.sellam.store.users.models.PermissionEnum;
import com.sellam.store.invoices.services.InvoiceService;
import com.sellam.store.sync.dto.SyncDTO;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.identity.repositories.ShopMembershipRepository;

@Service
public class SyncService
{

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final InvoiceService invoiceService;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;
    private final PersonRepository personRepository;
    private final ShopMembershipRepository shopMembershipRepository;

    public SyncService(InvoiceService invoiceService, org.springframework.transaction.support.TransactionTemplate transactionTemplate,
                      PersonRepository personRepository, ShopMembershipRepository shopMembershipRepository)
    {
        this.invoiceService = invoiceService;
        this.transactionTemplate = transactionTemplate;
        this.personRepository = personRepository;
        this.shopMembershipRepository = shopMembershipRepository;
    }

    public SyncDTO.SyncResponse processSync(SyncDTO.SyncRequest request, AuthPrincipal principal)
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
                    processRegisterSale(action, principal);
                }
                else if ("SYNC_INVOICE".equals(action.getType()))
                {
                    processSyncInvoice(action, principal);
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
    private void processSyncInvoice(SyncDTO.PendingAction action, AuthPrincipal principal)
    {
        transactionTemplate.execute(status -> {
            UUID shopId = UUID.fromString(action.getShopId());
            requireShopPermission(principal, shopId, PermissionEnum.CREATE_INVOICE);
            requireShopPermission(principal, shopId, PermissionEnum.VALIDATE_INVOICE);
            Map<String, Object> payload = action.getPayload();

            String customerName = (String) payload.get("customerName");
            String localCreatedAt = action.getLocalCreatedAt();
            String soldBy = principal.getId().toString();

            // 1. Create invoice
            InvoiceDTO.CreateInvoiceRequest createReq = InvoiceDTO.CreateInvoiceRequest.builder()
                    .customerName(customerName)
                    .localCreatedAt(localCreatedAt) // Passer la date locale
                    .soldBy(soldBy) // Passer l'ID utilisateur
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
                    if (line.get("lineDiscountAmount") != null || line.get("lineDiscountType") != null) {
                        requireShopPermission(principal, shopId, PermissionEnum.APPLY_LINE_DISCOUNT);
                    }
                    if (line.get("lineDiscountAmount") != null) {
                        lineReq.discountValue(new BigDecimal(line.get("lineDiscountAmount").toString()));
                    }
                    if (line.get("lineDiscountType") != null) {
                        lineReq.discountType((String) line.get("lineDiscountType"));
                    }
                    
                    invoiceService.addLine(invoice.getId(), lineReq.build(), null);
                }
            }
            
            // 3. Apply global discount if any
            if (payload.get("discountAmount") != null) {
                requireShopPermission(principal, shopId, PermissionEnum.APPLY_GLOBAL_DISCOUNT);
                BigDecimal discountAmt = new BigDecimal(payload.get("discountAmount").toString());
                String discountType = (String) payload.get("discountType");
                
                InvoiceDTO.ApplyInvoiceDiscountRequest discountReq = InvoiceDTO.ApplyInvoiceDiscountRequest.builder()
                        .discountValue(discountAmt)
                        .discountType(discountType)
                        .build();
                invoiceService.applyInvoiceDiscount(invoice.getId(), discountReq, null);
            }

            String paymentMethod = (String) payload.get("paymentMethod");

            // 4. Validate invoice
            InvoiceDTO.ValidateInvoiceRequest validateReq = InvoiceDTO.ValidateInvoiceRequest.builder()
                    .customerName(customerName)
                    .paymentMethod(paymentMethod)
                    .build();

            // Créer un faux AuthPrincipal avec l'ID utilisateur réel pour attribution correcte
            com.sellam.store.common.security.AuthPrincipal fakePrincipal = null;
            if (soldBy != null) {
                try {
                    UUID personId = UUID.fromString(soldBy);

                    // VALIDATION : Vérifier que la personne existe et a accès à la boutique
                    Optional<com.sellam.store.identity.models.PersonEntity> personOpt = personRepository.findById(personId);
                    if (personOpt.isEmpty()) {
                        log.warn("[SYNC] Person introuvable pour soldBy={}", soldBy);
                        throw new IllegalArgumentException("Person introuvable: " + soldBy);
                    }

                    com.sellam.store.identity.models.PersonEntity person = personOpt.get();
                    Optional<com.sellam.store.identity.models.ShopMembershipEntity> membershipOpt = shopMembershipRepository.findActiveMembership(personId, shopId);
                    if (membershipOpt.isEmpty()) {
                        log.warn("[SYNC] Person {} n'a pas de membership actif pour la boutique {}", personId, shopId);
                        throw new IllegalArgumentException("Person n'a pas accès à cette boutique");
                    }

                    // Récupérer le nom réel de la personne pour l'historique
                    String personName = person.getName() != null ? person.getName() : "Unknown";

                    fakePrincipal = com.sellam.store.common.security.AuthPrincipal.builder()
                            .id(personId)
                            .name(personName)
                            .userType("PERSON") // Type correct pour le nouveau modèle
                            .shopId(shopId)
                            .build();
                } catch (IllegalArgumentException e) {
                    log.warn("[SYNC] Validation soldBy échouée: {}", e.getMessage());
                    throw e; // Rejeter la synchronisation si validation échoue
                } catch (Exception e) {
                    log.warn("[SYNC] Erreur lors de la validation soldBy={}: {}", soldBy, e.getMessage());
                    throw new IllegalArgumentException("Format de soldBy invalide");
                }
            }

            invoiceService.validateInvoice(invoice.getId(), validateReq, fakePrincipal);
            return null;
        });
    }

    private void requireShopPermission(AuthPrincipal principal, UUID shopId, PermissionEnum permission)
    {
        if (principal != null && "ACCOUNT".equals(principal.getUserType()))
        {
            return;
        }
        var membership = shopMembershipRepository.findActiveMembership(principal.getId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Vous n'avez pas accès à cette boutique."));
        if (membership.getEffectivePermissions() == null || !membership.getEffectivePermissions().contains(permission))
        {
            throw new IllegalArgumentException("Vous n'avez pas le droit d'enregistrer cette vente.");
        }
    }

    private void processRegisterSale(SyncDTO.PendingAction action, AuthPrincipal principal)
    {
        transactionTemplate.execute(status -> {
            UUID shopId = UUID.fromString(action.getShopId());
            requireShopPermission(principal, shopId, PermissionEnum.CREATE_INVOICE);
            requireShopPermission(principal, shopId, PermissionEnum.VALIDATE_INVOICE);
            UUID productId = UUID.fromString((String) action.getPayload().get("productId"));
            BigDecimal quantity = new BigDecimal(action.getPayload().get("quantity").toString());

            InvoiceDTO.CreateInvoiceRequest createReq = InvoiceDTO.CreateInvoiceRequest.builder()
                    .customerName(null)
                    .build();
            InvoiceDTO.InvoiceResponse invoice = invoiceService.createInvoice(shopId, createReq);

            InvoiceDTO.AddLineRequest lineReq = InvoiceDTO.AddLineRequest.builder()
                    .productId(productId)
                    .quantity(quantity)
                    .build();
            invoiceService.addLine(invoice.getId(), lineReq, null);

            invoiceService.validateInvoice(invoice.getId(), null, null);
            return null;
        });
    }
}