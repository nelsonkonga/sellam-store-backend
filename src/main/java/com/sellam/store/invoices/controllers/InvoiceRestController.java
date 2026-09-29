package com.sellam.store.invoices.controllers;

import com.sellam.store.common.security.AuthPrincipal;
import com.sellam.store.common.security.IShopAccessGuard;
import com.sellam.store.invoices.dto.InvoiceDTO;
import com.sellam.store.subscriptions.security.SubscriptionWriteGuard;
import com.sellam.store.invoices.services.InvoiceService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/invoices")
public class InvoiceRestController
{

    private final InvoiceService invoiceService;
    private final IShopAccessGuard shopAccessGuard;
    private final SubscriptionWriteGuard subscriptionWriteGuard;

    @PreAuthorize("@sec.can(authentication, 'CREATE_INVOICE')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceDTO.InvoiceResponse create(@RequestParam UUID shopId,
                                             @RequestBody InvoiceDTO.CreateInvoiceRequest request,
                                             Authentication authentication)
    {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        subscriptionWriteGuard.assertWritable(shopId);
        return invoiceService.createInvoice(shopId, request);
    }

    @PreAuthorize("@sec.can(authentication, 'EDIT_INVOICE')")
    @PostMapping("/{id}/lines")
    @ResponseStatus(HttpStatus.OK)
    public InvoiceDTO.InvoiceResponse addLine(@PathVariable UUID id,
                                              @RequestBody InvoiceDTO.AddLineRequest request,
                                              Authentication authentication)
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);
        UUID invoiceShopId = invoiceService.getInvoiceShopId(id);
        shopAccessGuard.checkShopAccess(principal, invoiceShopId);
        subscriptionWriteGuard.assertWritable(invoiceShopId);
        return invoiceService.addLine(id, request, principal);
    }

    @PreAuthorize("@sec.can(authentication, 'EDIT_INVOICE')")
    @PutMapping("/{id}/lines/{saleId}/quantity")
    @ResponseStatus(HttpStatus.OK)
    public InvoiceDTO.InvoiceResponse modifyLineQuantity(@PathVariable UUID id,
                                                         @PathVariable UUID saleId,
                                                         @RequestBody InvoiceDTO.ModifyQuantityRequest request,
                                                         Authentication authentication)
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);
        UUID invoiceShopId = invoiceService.getInvoiceShopId(id);
        shopAccessGuard.checkShopAccess(principal, invoiceShopId);
        subscriptionWriteGuard.assertWritable(invoiceShopId);
        return invoiceService.modifyLineQuantity(id, saleId, request.getQuantity(), principal);
    }

    @PreAuthorize("@sec.can(authentication, 'APPLY_LINE_DISCOUNT')")
    @PostMapping("/{id}/lines/{saleId}/discount")
    @ResponseStatus(HttpStatus.OK)
    public InvoiceDTO.InvoiceResponse applyLineDiscount(@PathVariable UUID id,
                                                        @PathVariable UUID saleId,
                                                        @RequestBody InvoiceDTO.ApplyLineDiscountRequest request,
                                                        Authentication authentication)
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);
        UUID invoiceShopId = invoiceService.getInvoiceShopId(id);
        shopAccessGuard.checkShopAccess(principal, invoiceShopId);
        subscriptionWriteGuard.assertWritable(invoiceShopId);
        return invoiceService.applyLineDiscount(id, saleId, request, principal);
    }

    @PreAuthorize("@sec.can(authentication, 'DELETE_INVOICE_LINE')")
    @DeleteMapping("/{id}/lines/{saleId}")
    @ResponseStatus(HttpStatus.OK)
    public InvoiceDTO.InvoiceResponse removeLine(@PathVariable UUID id, @PathVariable UUID saleId,
                                                 @RequestParam(defaultValue = "false") boolean isManagerAction,
                                                 Authentication authentication)
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);
        UUID invoiceShopId = invoiceService.getInvoiceShopId(id);
        shopAccessGuard.checkShopAccess(principal, invoiceShopId);
        subscriptionWriteGuard.assertWritable(invoiceShopId);
        return invoiceService.removeLine(id, saleId, isManagerAction, principal);
    }

    @PreAuthorize("@sec.can(authentication, 'APPLY_GLOBAL_DISCOUNT')")
    @PostMapping("/{id}/discount")
    @ResponseStatus(HttpStatus.OK)
    public InvoiceDTO.InvoiceResponse applyDiscount(@PathVariable UUID id,
                                                    @RequestBody InvoiceDTO.ApplyInvoiceDiscountRequest request,
                                                    Authentication authentication)
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);
        UUID invoiceShopId = invoiceService.getInvoiceShopId(id);
        shopAccessGuard.checkShopAccess(principal, invoiceShopId);
        subscriptionWriteGuard.assertWritable(invoiceShopId);
        return invoiceService.applyInvoiceDiscount(id, request, principal);
    }

    @PreAuthorize("@sec.can(authentication, 'VALIDATE_INVOICE')")
    @PostMapping("/{id}/validate")
    @ResponseStatus(HttpStatus.OK)
    public InvoiceDTO.InvoiceResponse validate(@PathVariable UUID id,
                                               @RequestBody(required = false) InvoiceDTO.ValidateInvoiceRequest request,
                                               Authentication authentication)
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);
        UUID invoiceShopId = invoiceService.getInvoiceShopId(id);
        shopAccessGuard.checkShopAccess(principal, invoiceShopId);
        subscriptionWriteGuard.assertWritable(invoiceShopId);
        return invoiceService.validateInvoice(id, request, principal);
    }

    // Vérification d'accès à la boutique requise pour consulter/imprimer une facture
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public byte[] pdf(@PathVariable UUID id, HttpServletResponse response, Authentication authentication)
    {
        UUID shopId = invoiceService.getInvoiceShopId(id);
        shopAccessGuard.checkShopAccess(authentication, shopId);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, String.format("inline; filename=facture-%s.pdf", id.toString()));
        return invoiceService.generatePdf(id);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public List<InvoiceDTO.InvoiceResponse> list(@RequestParam UUID shopId, Authentication authentication)
    {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        return invoiceService.listInvoices(shopId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public InvoiceDTO.InvoiceResponse getInvoice(@PathVariable UUID id, Authentication authentication)
    {
        UUID shopId = invoiceService.getInvoiceShopId(id);
        shopAccessGuard.checkShopAccess(authentication, shopId);
        return invoiceService.getInvoice(id);
    }
}