package com.sellam.store.invoices.services;

import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.invoices.dto.InvoiceDTO;
import com.sellam.store.invoices.models.*;
import com.sellam.store.invoices.repositories.InvoiceCounterRepository;
import com.sellam.store.invoices.repositories.InvoiceRepository;
import com.sellam.store.products.models.ProductEntity;
import com.sellam.store.products.repositories.ProductsRepository;
import com.sellam.store.sales.models.SaleEntity;
import com.sellam.store.sales.models.SaleStatusEnum;
import com.sellam.store.sales.repositories.SalesRepository;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class InvoiceService
{

    private final InvoiceRepository invoiceRepository;
    private final InvoiceCounterRepository counterRepository;
    private final SalesRepository salesRepository;
    private final ProductsRepository productsRepository;
    private final ShopRepository shopRepository;


    @Transactional
    public InvoiceDTO.InvoiceResponse createInvoice(UUID shopId, InvoiceDTO.CreateInvoiceRequest request)
    {
        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique introuvable"));

        InvoiceEntity invoice = InvoiceEntity.builder()
                .invoiceNumber(generateInvoiceNumber(shop))
                .shop(shop)
                .customerName(request.getCustomerName())
                .subtotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .totalMargin(BigDecimal.ZERO)
                .status(InvoiceStatusEnum.OPEN)
                .build();

        InvoiceEntity saved = invoiceRepository.save(invoice);
        return toResponse(saved, List.of());
    }

    @Transactional
    public InvoiceDTO.InvoiceResponse addLine(UUID invoiceId, InvoiceDTO.AddLineRequest request)
    {
        InvoiceEntity invoice = getEditableInvoice(invoiceId);

        ProductEntity product = productsRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable"));

        if (product.getStockQuantity().compareTo(request.getQuantity()) < 0) {
            throw new IllegalArgumentException("Stock insuffisant pour " + product.getName());
        }

        BigDecimal lineSubtotal = product.getSellingPrice().multiply(request.getQuantity());
        BigDecimal lineDiscount = computeDiscount(lineSubtotal, request.getDiscountType(), request.getDiscountValue());
        BigDecimal lineTotal = lineSubtotal.subtract(lineDiscount);
        BigDecimal lineMargin = product.getSellingPrice().subtract(product.getPurchasePrice())
                .multiply(request.getQuantity()).subtract(lineDiscount);

        SaleEntity sale = SaleEntity.builder()
                .invoice(invoice)
                .product(product)
                .shop(invoice.getShop())
                .quantity(request.getQuantity())
                .lineSubtotal(lineSubtotal)
                .discountType(parseDiscountType(request.getDiscountType()))
                .discountValue(request.getDiscountValue())
                .discountAmount(lineDiscount)
                .totalPrice(lineTotal)
                .margin(lineMargin)
                .status(SaleStatusEnum.valueOf("CONFIRMED"))
                .build();

        salesRepository.save(sale);


        product.setStockQuantity(product.getStockQuantity().subtract(request.getQuantity()));
        productsRepository.save(product);

        recomputeInvoiceTotals(invoice);
        return toResponse(invoice, salesRepository.findByInvoice_Id(invoiceId));
    }

    @Transactional
    public InvoiceDTO.InvoiceResponse removeLine(UUID invoiceId, UUID saleId, boolean isManagerAction)
    {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));

        if (invoice.getStatus() == InvoiceStatusEnum.VALIDATED && !isManagerAction)
        {
            throw new IllegalArgumentException("Seul le gérant peut modifier une facture déjà validée");
        }

        SaleEntity sale = salesRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable"));


        ProductEntity product = sale.getProduct();
        product.setStockQuantity(product.getStockQuantity().add(sale.getQuantity()));
        productsRepository.save(product);

        salesRepository.delete(sale);

        recomputeInvoiceTotals(invoice);
        return toResponse(invoice, salesRepository.findByInvoice_Id(invoiceId));
    }

    @Transactional
    public InvoiceDTO.InvoiceResponse applyInvoiceDiscount(UUID invoiceId, InvoiceDTO.ApplyInvoiceDiscountRequest request)
    {
        InvoiceEntity invoice = getEditableInvoice(invoiceId);
        invoice.setDiscountType(DiscountTypeEnum.valueOf(request.getDiscountType()));
        invoice.setDiscountValue(request.getDiscountValue());
        recomputeInvoiceTotals(invoice);
        return toResponse(invoice, salesRepository.findByInvoice_Id(invoiceId));
    }

    @Transactional
    public InvoiceDTO.InvoiceResponse validateInvoice(UUID invoiceId)
    {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));

        List<SaleEntity> lines = salesRepository.findByInvoice_Id(invoiceId);
        if (lines.isEmpty())
        {
            throw new IllegalArgumentException("Impossible de valider une facture vide");
        }

        invoice.setStatus(InvoiceStatusEnum.VALIDATED);
        InvoiceEntity saved = invoiceRepository.save(invoice);
        return toResponse(saved, lines);
    }

    private InvoiceEntity getEditableInvoice(UUID invoiceId)
    {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));
    }

    private void recomputeInvoiceTotals(InvoiceEntity invoice)
    {
        List<SaleEntity> lines = salesRepository.findByInvoice_Id(invoice.getId());

        BigDecimal subtotal = lines.stream().map(SaleEntity::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal marginSum = lines.stream().map(SaleEntity::getMargin)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal invoiceDiscount = computeDiscount(subtotal,
                invoice.getDiscountType() != null ? invoice.getDiscountType().name() : null,
                invoice.getDiscountValue());

        invoice.setSubtotal(subtotal);
        invoice.setDiscountAmount(invoiceDiscount);
        invoice.setTotalAmount(subtotal.subtract(invoiceDiscount));
        invoice.setTotalMargin(marginSum.subtract(invoiceDiscount));
        invoiceRepository.save(invoice);
    }

    private BigDecimal computeDiscount(BigDecimal base, String type, BigDecimal value)
    {
        if (type == null || value == null) return BigDecimal.ZERO;
        if ("PERCENTAGE".equals(type))
        {
            return base.multiply(value).divide(BigDecimal.valueOf(100));
        }
        return value;
    }

    private DiscountTypeEnum parseDiscountType(String type)
    {
        return type != null ? DiscountTypeEnum.valueOf(type) : null;
    }

    private String generateInvoiceNumber(ShopEntity shop)
    {
        InvoiceCounterEntity counter = counterRepository.findByShop_Id(shop.getId()).orElse(null);
        if (counter == null) {
            counter = InvoiceCounterEntity.builder().shop(shop).lastNumber(0).build();
            // On flush tout de suite pour que la prochaine requête dans la même transaction le trouve
            counterRepository.saveAndFlush(counter);
        }
        counter.setLastNumber(counter.getLastNumber() + 1);
        counterRepository.saveAndFlush(counter);
        return String.format("F-%d-%04d", LocalDate.now().getYear(), counter.getLastNumber());
    }

    private InvoiceDTO.InvoiceResponse toResponse(InvoiceEntity invoice, List<SaleEntity> lines)
    {
        List<InvoiceDTO.LineResponse> lineResponses = lines.stream()
                .map(s -> InvoiceDTO.LineResponse.builder()
                        .saleId(s.getId())
                        .productName(s.getProduct().getName())
                        .quantity(s.getQuantity())
                        .lineSubtotal(s.getLineSubtotal())
                        .discountAmount(s.getDiscountAmount())
                        .totalPrice(s.getTotalPrice())
                        .build())
                .collect(Collectors.toList());

        return InvoiceDTO.InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .customerName(invoice.getCustomerName())
                .lines(lineResponses)
                .subtotal(invoice.getSubtotal())
                .discountAmount(invoice.getDiscountAmount())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus().name())
                .createdAt(invoice.getCreatedAt())
                .build();
    }

    public List<InvoiceDTO.InvoiceResponse> listInvoices(UUID shopId)
    {
        return invoiceRepository.findByShop_IdAndStatusOrderByCreatedAtDesc(shopId, InvoiceStatusEnum.VALIDATED)
                .stream()
                .map(inv -> toResponse(inv, salesRepository.findByInvoice_Id(inv.getId())))
                .collect(Collectors.toList());
    }


    public byte[] generatePdf(UUID invoiceId)
    {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));
        List<SaleEntity> lines = salesRepository.findByInvoice_Id(invoiceId);

        try
        {
            float width = 226f; // 80mm
            float height = 350f + (lines.size() * 25f);

            Document document = new Document(new Rectangle(width, height), 10, 10, 10, 10);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

            Paragraph shopName = new Paragraph(invoice.getShop().getName(), titleFont);
            shopName.setAlignment(Element.ALIGN_CENTER);
            document.add(shopName);

            if (invoice.getShop().getAddress() != null)
            {
                Paragraph address = new Paragraph(invoice.getShop().getAddress(), normalFont);
                address.setAlignment(Element.ALIGN_CENTER);
                document.add(address);
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Facture N° : " + invoice.getInvoiceNumber(), normalFont));
            document.add(new Paragraph("Date : " + invoice.getCreatedAt()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), normalFont));
            if (invoice.getCustomerName() != null && !invoice.getCustomerName().isBlank())
            {
                document.add(new Paragraph("Client : " + invoice.getCustomerName(), normalFont));
            }
            document.add(new Paragraph("--------------------------------", normalFont));

            for (SaleEntity sale : lines) {
                document.add(new Paragraph(sale.getProduct().getName(), boldFont));
                String lineText = "  " + sale.getQuantity() +
                        " x " + sale.getProduct().getSellingPrice() +
                        " = " + sale.getLineSubtotal() + " FCFA";
                document.add(new Paragraph(lineText, normalFont));

                if (sale.getDiscountAmount() != null && sale.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0)
                {
                    document.add(new Paragraph("  Remise : -" + sale.getDiscountAmount() + " FCFA", normalFont));
                }
            }

            document.add(new Paragraph("--------------------------------", normalFont));
            document.add(new Paragraph("Sous-total : " + invoice.getSubtotal() + " FCFA", normalFont));

            if (invoice.getDiscountAmount() != null && invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0)
            {
                document.add(new Paragraph("Remise facture : -" + invoice.getDiscountAmount() + " FCFA", normalFont));
            }

            document.add(new Paragraph("TOTAL : " + invoice.getTotalAmount() + " FCFA", totalFont));
            document.add(new Paragraph(" "));

            Paragraph thanks = new Paragraph("Merci de votre confiance !", normalFont);
            thanks.setAlignment(Element.ALIGN_CENTER);
            document.add(thanks);

            document.close();
            return out.toByteArray();

        } catch (DocumentException e)
        {
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }

    public InvoiceDTO.InvoiceResponse getInvoice(UUID invoiceId)
    {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));
        return toResponse(invoice, salesRepository.findByInvoice_Id(invoiceId));
    }

}