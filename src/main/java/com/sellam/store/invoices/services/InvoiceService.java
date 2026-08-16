package com.sellam.store.invoices.services;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sellam.store.accounts.models.AccountEntity;
import com.sellam.store.accounts.repositories.AccountRepository;
import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.common.security.AuthPrincipal;
import com.sellam.store.invoices.dto.InvoiceDTO;
import com.sellam.store.invoices.models.DiscountTypeEnum;
import com.sellam.store.invoices.models.InvoiceCounterEntity;
import com.sellam.store.invoices.models.InvoiceEntity;
import com.sellam.store.invoices.models.InvoiceStatusEnum;
import com.sellam.store.invoices.repositories.InvoiceCounterRepository;
import com.sellam.store.invoices.repositories.InvoiceRepository;
import com.sellam.store.products.models.ProductEntity;
import com.sellam.store.products.repositories.ProductsRepository;
import com.sellam.store.sales.models.SaleEntity;
import com.sellam.store.sales.models.SaleStatusEnum;
import com.sellam.store.sales.repositories.SalesRepository;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import com.sellam.store.users.models.UserEntity;
import com.sellam.store.users.repositories.UserRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class InvoiceService
{

    private final InvoiceRepository invoiceRepository;
    private final InvoiceCounterRepository counterRepository;
    private final SalesRepository salesRepository;
    private final ProductsRepository productsRepository;
    private final ShopRepository shopRepository;
    private final AccountRepository accountRepository;
    private final UserRepository usersRepository;


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

    /**
     * Retire une ligne de facture. Si la facture ne contient plus aucune
     * ligne après suppression, elle est elle-même supprimée (une facture
     * à zéro produit n'a pas de sens métier).
     */
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

        List<SaleEntity> remainingLines = salesRepository.findByInvoice_Id(invoiceId);

        if (remainingLines.isEmpty())
        {
            invoiceRepository.delete(invoice);
            return toResponse(invoice, List.of());
        }

        recomputeInvoiceTotals(invoice);
        return toResponse(invoice, remainingLines);
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
        return validateInvoice(invoiceId, null);
    }

    @Transactional
    public InvoiceDTO.InvoiceResponse validateInvoice(UUID invoiceId, AuthPrincipal principal)
    {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));

        List<SaleEntity> lines = salesRepository.findByInvoice_Id(invoiceId);
        if (lines.isEmpty())
        {
            throw new IllegalArgumentException("Impossible de valider une facture vide");
        }

        String validatorName = resolveValidatorName(principal);

        invoice.setStatus(InvoiceStatusEnum.VALIDATED);
        invoice.setValidatedByName(validatorName);
        InvoiceEntity saved = invoiceRepository.save(invoice);
        return toResponse(saved, lines);
    }

    /**
     * Résout le nom à afficher pour le validateur de la facture, selon
     * qu'il s'agit d'un employé (UserEntity) ou du compte propriétaire
     * (AccountEntity, sans ligne dans la table users).
     */
    private String resolveValidatorName(AuthPrincipal principal)
    {
        if ("ACCOUNT".equals(principal.getUserType()))
        {
            return accountRepository.findById(principal.getId())
                    .map(AccountEntity::getName)
                    .orElse("Compte propriétaire");
        }

        return usersRepository.findById(principal.getId())
                .map(UserEntity::getName)
                .orElse("Employé");
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
            return base.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
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
                .status(invoice.getStatus() != null ? invoice.getStatus().name() : null)
                .createdAt(invoice.getCreatedAt())
                .validatedByName(invoice.getValidatedByName())
                .build();
    }

    public List<InvoiceDTO.InvoiceResponse> listInvoices(UUID shopId)
    {
        return invoiceRepository.findByShop_IdAndStatusOrderByCreatedAtDesc(shopId, InvoiceStatusEnum.VALIDATED)
                .stream()
                .map(inv -> toResponse(inv, salesRepository.findByInvoice_Id(inv.getId())))
                .collect(Collectors.toList());
    }

    // ================================================================
    // GÉNÉRATION PDF — ticket 80mm, tableau avec colonnes, logo en en-tête
    // ================================================================

    private static final float PAGE_WIDTH_MM = 80f;
    private static final float PAGE_WIDTH_PT = PAGE_WIDTH_MM * 72f / 25.4f;
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public byte[] generatePdf(UUID invoiceId)
    {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));
        List<SaleEntity> lines = salesRepository.findByInvoice_Id(invoiceId);
        ShopEntity shop = invoice.getShop();

        float estimatedHeight = 420f + (lines.size() * 34f);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream())
        {
            Document document = new Document(new Rectangle(PAGE_WIDTH_PT, estimatedHeight), 8, 8, 10, 10);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 7);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7);
            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            addHeader(document, shop, titleFont, normalFont, smallFont);
            addInvoiceMeta(document, invoice, normalFont, boldFont);
            addLinesTable(document, lines, tableHeaderFont, normalFont, boldFont);
            addTotals(document, invoice, normalFont, boldFont, totalFont);
            addFooter(document, invoice, smallFont);

            document.close();
            return out.toByteArray();
        }
        catch (DocumentException | java.io.IOException e)
        {
            log.error("Erreur lors de la génération du PDF pour la facture {} : {}", invoiceId, e.getMessage());
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }

    private void addHeader(Document document, ShopEntity shop, Font titleFont, Font normalFont, Font smallFont)
            throws DocumentException
    {
        Image logo = tryLoadLogo(shop.getLogoUrl());
        if (logo != null)
        {
            logo.scaleToFit(80f, 80f);
            logo.setAlignment(Element.ALIGN_CENTER);
            document.add(logo);
        }

        Paragraph shopName = new Paragraph(shop.getName(), titleFont);
        shopName.setAlignment(Element.ALIGN_CENTER);
        document.add(shopName);

        if (shop.getAddress() != null && !shop.getAddress().isBlank())
        {
            document.add(centered(shop.getAddress(), smallFont));
        }
        if (shop.getPhoneNumber() != null && !shop.getPhoneNumber().isBlank())
        {
            document.add(centered("Tél : " + shop.getPhoneNumber(), smallFont));
        }
        if (shop.getTaxpayerNumber() != null && !shop.getTaxpayerNumber().isBlank())
        {
            document.add(centered("N° contribuable : " + shop.getTaxpayerNumber(), smallFont));
        }

        document.add(separator(normalFont));
    }

    private Image tryLoadLogo(String logoUrl)
    {
        if (logoUrl == null || logoUrl.isBlank())
        {
            return null;
        }

        try
        {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(logoUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200)
            {
                log.warn("Logo introuvable ({}) à l'URL {}", response.statusCode(), logoUrl);
                return null;
            }

            return Image.getInstance(response.body());
        }
        catch (Exception e)
        {
            log.warn("Impossible de charger le logo depuis {} : {}", logoUrl, e.getMessage());
            return null;
        }
    }

    private void addInvoiceMeta(Document document, InvoiceEntity invoice, Font normalFont, Font boldFont)
            throws DocumentException
    {
        document.add(new Paragraph("Facture N° : " + invoice.getInvoiceNumber(), boldFont));
        document.add(new Paragraph("Date : " + invoice.getCreatedAt().format(DATE_TIME_FORMAT), normalFont));

        if (invoice.getCustomerName() != null && !invoice.getCustomerName().isBlank())
        {
            document.add(new Paragraph("Client : " + invoice.getCustomerName(), normalFont));
        }

        if (invoice.getValidatedByName() != null && !invoice.getValidatedByName().isBlank())
        {
            document.add(new Paragraph("Émise par : " + invoice.getValidatedByName(), normalFont));
        }

        document.add(separator(normalFont));
    }

    private void addLinesTable(Document document, List<SaleEntity> lines,
                                Font headerFont, Font normalFont, Font boldFont) throws DocumentException
    {
        PdfPTable table = new PdfPTable(new float[]{ 2.6f, 0.7f, 1.1f, 1.1f });
        table.setWidthPercentage(100);
        table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        addHeaderCell(table, "Article", headerFont);
        addHeaderCell(table, "Qté", headerFont);
        addHeaderCell(table, "P.U.", headerFont);
        addHeaderCell(table, "Total", headerFont);

        for (SaleEntity sale : lines)
        {
            addBodyCell(table, sale.getProduct().getName(), normalFont, Element.ALIGN_LEFT);
            addBodyCell(table, formatQuantity(sale.getQuantity()), normalFont, Element.ALIGN_CENTER);
            addBodyCell(table, formatAmount(sale.getProduct().getSellingPrice()), normalFont, Element.ALIGN_RIGHT);
            addBodyCell(table, formatAmount(sale.getLineSubtotal()), boldFont, Element.ALIGN_RIGHT);

            if (sale.getDiscountAmount() != null && sale.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0)
            {
                PdfPCell discountCell = new PdfPCell(new Phrase(
                        "  Remise : -" + formatAmount(sale.getDiscountAmount()) + " FCFA",
                        FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7)));
                discountCell.setColspan(4);
                discountCell.setBorder(Rectangle.NO_BORDER);
                discountCell.setPaddingBottom(3f);
                table.addCell(discountCell);
            }
        }

        document.add(table);
        document.add(separator(normalFont));
    }

    private void addHeaderCell(PdfPTable table, String text, Font font)
    {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setPaddingBottom(3f);
        cell.setHorizontalAlignment(text.equals("Article") ? Element.ALIGN_LEFT : Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, Font font, int alignment)
    {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(2f);
        cell.setPaddingBottom(2f);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private void addTotals(Document document, InvoiceEntity invoice, Font normalFont, Font boldFont, Font totalFont)
            throws DocumentException
    {
        document.add(rightAligned("Sous-total : " + formatAmount(invoice.getSubtotal()) + " FCFA", normalFont));

        if (invoice.getDiscountAmount() != null && invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0)
        {
            document.add(rightAligned("Remise facture : -" + formatAmount(invoice.getDiscountAmount()) + " FCFA", normalFont));
        }

        document.add(rightAligned("TOTAL : " + formatAmount(invoice.getTotalAmount()) + " FCFA", totalFont));
        document.add(new Paragraph(" "));
    }

    private void addFooter(Document document, InvoiceEntity invoice, Font smallFont) throws DocumentException
    {
        document.add(centered("Merci de votre confiance !", smallFont));
    }

    private Paragraph centered(String text, Font font)
    {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    private Paragraph rightAligned(String text, Font font)
    {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }

    private Paragraph separator(Font font)
    {
        return new Paragraph("--------------------------------", font);
    }

    private String formatAmount(BigDecimal amount)
    {
        if (amount == null)
        {
            return "0";
        }
        return amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatQuantity(BigDecimal quantity)
    {
        if (quantity == null)
        {
            return "0";
        }
        if (quantity.stripTrailingZeros().scale() <= 0)
        {
            return quantity.setScale(0, RoundingMode.HALF_UP).toPlainString();
        }
        return quantity.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public InvoiceDTO.InvoiceResponse getInvoice(UUID invoiceId)
    {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));
        return toResponse(invoice, salesRepository.findByInvoice_Id(invoiceId));
    }

}