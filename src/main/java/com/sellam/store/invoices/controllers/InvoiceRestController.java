package com.sellam.store.invoices.controllers;

import com.sellam.store.invoices.dto.InvoiceDTO;
import com.sellam.store.invoices.services.InvoiceService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/invoices")
public class InvoiceRestController
{

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<InvoiceDTO.InvoiceResponse> create(@RequestParam UUID shopId,
                                                             @RequestBody InvoiceDTO.CreateInvoiceRequest request)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.createInvoice(shopId, request));
    }

    @PostMapping("/{id}/lines")
    public ResponseEntity<InvoiceDTO.InvoiceResponse> addLine(@PathVariable UUID id,
                                                              @RequestBody InvoiceDTO.AddLineRequest request)
    {
        return ResponseEntity.ok(invoiceService.addLine(id, request));
    }

    @DeleteMapping("/{id}/lines/{saleId}")
    public ResponseEntity<InvoiceDTO.InvoiceResponse> removeLine(@PathVariable UUID id, @PathVariable UUID saleId,
                                                                 @RequestParam(defaultValue = "false") boolean isManagerAction)
    {
        return ResponseEntity.ok(invoiceService.removeLine(id, saleId, isManagerAction));
    }

    @PostMapping("/{id}/discount")
    public ResponseEntity<InvoiceDTO.InvoiceResponse> applyDiscount(@PathVariable UUID id,
                                                                    @RequestBody InvoiceDTO.ApplyInvoiceDiscountRequest request)
    {
        return ResponseEntity.ok(invoiceService.applyInvoiceDiscount(id, request));
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<InvoiceDTO.InvoiceResponse> validate(@PathVariable UUID id)
    {
        return ResponseEntity.ok(invoiceService.validateInvoice(id));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id)
    {
        byte[] pdf = invoiceService.generatePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=facture.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping
    public ResponseEntity<List<InvoiceDTO.InvoiceResponse>> list(@RequestParam UUID shopId)
    {
        return ResponseEntity.ok(invoiceService.listInvoices(shopId));
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public InvoiceDTO.InvoiceResponse getInvoice(@PathVariable UUID id)
    {
        return invoiceService.getInvoice(id);
    }
}