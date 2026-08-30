package com.sellam.store.invoices.controllers;

import com.sellam.store.common.security.AuthPrincipal;
import com.sellam.store.common.security.IShopAccessGuard;
import com.sellam.store.invoices.dto.InvoiceDTO;
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

    @PreAuthorize("@sec.can(authentication, 'CREATE_INVOICE')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceDTO.InvoiceResponse create(@RequestParam UUID shopId,
                                             @RequestBody InvoiceDTO.CreateInvoiceRequest request)
    {
        return invoiceService.createInvoice(shopId, request);
    }

    @PreAuthorize("@sec.can(authentication, 'EDIT_INVOICE')")
    @PostMapping("/{id}/lines")
    @ResponseStatus(HttpStatus.OK)
    public InvoiceDTO.InvoiceResponse addLine(@PathVariable UUID id,
                                              @RequestBody InvoiceDTO.AddLineRequest request)
    {
        return invoiceService.addLine(id, request);
    }

    @PreAuthorize("@sec.can(authentication, 'DELETE_INVOICE_LINE')")
    @DeleteMapping("/{id}/lines/{saleId}")
    @ResponseStatus(HttpStatus.OK)
    public InvoiceDTO.InvoiceResponse removeLine(@PathVariable UUID id, @PathVariable UUID saleId,
                                                 @RequestParam(defaultValue = "false") boolean isManagerAction)
    {
        return invoiceService.removeLine(id, saleId, isManagerAction);
    }

    @PreAuthorize("@sec.can(authentication, 'EDIT_INVOICE')")
    @PostMapping("/{id}/discount")
    @ResponseStatus(HttpStatus.OK)
    public InvoiceDTO.InvoiceResponse applyDiscount(@PathVariable UUID id,
                                                    @RequestBody InvoiceDTO.ApplyInvoiceDiscountRequest request)
    {
        return invoiceService.applyInvoiceDiscount(id, request);
    }

    @PreAuthorize("@sec.can(authentication, 'VALIDATE_INVOICE')")
    @PostMapping("/{id}/validate")
    @ResponseStatus(HttpStatus.OK)
    public InvoiceDTO.InvoiceResponse validate(@PathVariable UUID id,
                                               @RequestBody(required = false) InvoiceDTO.ValidateInvoiceRequest request,
                                               Authentication authentication)
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);
        return invoiceService.validateInvoice(id, request, principal);
    }

    // Pas de restriction de permission métier : consulter/imprimer une facture déjà
    // créée est une action de lecture peu sensible, laissée à authenticated() de base.
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public byte[] pdf(@PathVariable UUID id, HttpServletResponse response)
    {
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, String.format("inline; filename=facture-%s.pdf", id.toString()));
        return invoiceService.generatePdf(id);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<InvoiceDTO.InvoiceResponse> list(@RequestParam UUID shopId)
    {
        return invoiceService.listInvoices(shopId);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public InvoiceDTO.InvoiceResponse getInvoice(@PathVariable UUID id)
    {
        return invoiceService.getInvoice(id);
    }
}