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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.common.security.AuthPrincipal;
import com.sellam.store.invoices.dto.InvoiceDTO;
import com.sellam.store.invoices.models.DiscountTypeEnum;
import com.sellam.store.invoices.models.InvoiceCounterEntity;
import com.sellam.store.invoices.models.InvoiceEntity;
import com.sellam.store.invoices.models.InvoiceHistoryActionType;
import com.sellam.store.invoices.models.InvoiceHistoryEntity;
import com.sellam.store.invoices.models.InvoiceStatusEnum;
import com.sellam.store.invoices.repositories.InvoiceCounterRepository;
import com.sellam.store.invoices.repositories.InvoiceHistoryRepository;
import com.sellam.store.invoices.repositories.InvoiceRepository;
import com.sellam.store.dailybalance.services.DailyBalanceService;
import com.sellam.store.products.models.ProductEntity;
import com.sellam.store.products.repositories.ProductsRepository;
import com.sellam.store.sales.models.SaleEntity;
import com.sellam.store.sales.models.SaleStatusEnum;
import com.sellam.store.sales.repositories.SalesRepository;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
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
    private final PersonRepository personRepository;
    private final InvoiceHistoryRepository invoiceHistoryRepository;
    private final DailyBalanceService dailyBalanceService;


    @Transactional
    public InvoiceDTO.InvoiceResponse createInvoice(UUID shopId, InvoiceDTO.CreateInvoiceRequest request)
    {
        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique introuvable"));

        // Validation de la date locale si fournie (pour la synchronisation hors ligne)
        LocalDateTime createdAt = null;
        if (request.getLocalCreatedAt() != null) {
            try {
                createdAt = LocalDateTime.parse(request.getLocalCreatedAt());
                LocalDateTime now = LocalDateTime.now();

                // Rejeter les dates dans le futur (tolérance 1 heure pour désynchronisation horaire)
                if (createdAt.isAfter(now.plusHours(1))) {
                    throw new IllegalArgumentException("La date de création ne peut pas être dans le futur");
                }

                // Rejeter les dates trop anciennes (plus de 30 jours)
                if (createdAt.isBefore(now.minusDays(30))) {
                    throw new IllegalArgumentException("La date de création est trop ancienne (plus de 30 jours)");
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Format de date invalide: " + request.getLocalCreatedAt());
            }
        }

        InvoiceEntity.InvoiceEntityBuilder builder = InvoiceEntity.builder()
                .invoiceNumber(generateInvoiceNumber(shop))
                .shop(shop)
                .customerName(request.getCustomerName())
                .subtotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .totalMargin(BigDecimal.ZERO)
                .status(InvoiceStatusEnum.OPEN);

        // Utiliser la date locale si fournie, sinon laisser @CreatedDate s'appliquer
        if (createdAt != null) {
            builder.createdAt(createdAt);
        }

        // Utiliser l'ID utilisateur fourni (pour la synchronisation)
        if (request.getSoldBy() != null) {
            builder.soldBy(request.getSoldBy());
        }

        InvoiceEntity invoice = builder.build();

        InvoiceEntity saved = invoiceRepository.save(invoice);
        return toResponse(saved, List.of());
    }

    @Transactional
    public InvoiceDTO.InvoiceResponse addLine(UUID invoiceId, InvoiceDTO.AddLineRequest request, AuthPrincipal principal)
    {
        InvoiceEntity invoice = getEditableInvoice(invoiceId);

        ProductEntity product = productsRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable"));

        // Vérifier si le produit existe déjà sur la facture
        SaleEntity existingLine = salesRepository.findByInvoice_IdAndProduct_Id(invoiceId, request.getProductId());

        if (existingLine != null) {
            // Le produit existe déjà : additionner la quantité
            BigDecimal oldQuantity = existingLine.getQuantity();
            BigDecimal newQuantity = oldQuantity.add(request.getQuantity());

            if (product.getStockQuantity().compareTo(request.getQuantity()) < 0) {
                throw new IllegalArgumentException("Stock insuffisant pour " + product.getName());
            }

            existingLine.setQuantity(newQuantity);

            // Recalculer les montants de la ligne
            BigDecimal lineSubtotal = product.getSellingPrice().multiply(newQuantity);
            BigDecimal lineDiscount = computeDiscount(lineSubtotal, existingLine.getDiscountType() != null ? existingLine.getDiscountType().name() : null, existingLine.getDiscountValue());
            BigDecimal lineTotal = lineSubtotal.subtract(lineDiscount);
            BigDecimal lineMargin = product.getSellingPrice().subtract(product.getPurchasePrice())
                    .multiply(newQuantity).subtract(lineDiscount);

            existingLine.setLineSubtotal(lineSubtotal);
            existingLine.setDiscountAmount(lineDiscount);
            existingLine.setTotalPrice(lineTotal);
            existingLine.setMargin(lineMargin);

            salesRepository.save(existingLine);

            product.setStockQuantity(product.getStockQuantity().subtract(request.getQuantity()));
            productsRepository.save(product);

            recomputeInvoiceTotals(invoice);

            // Enregistrer l'historique
            String details = String.format("Produit: %s, Quantité: %s -> %s", product.getName(), oldQuantity, newQuantity);
            recordHistory(invoice, InvoiceHistoryActionType.ADD_LINE, details, principal);

            return toResponse(invoice, salesRepository.findByInvoice_Id(invoiceId));
        }

        // Nouvelle ligne : créer comme avant
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

        // Enregistrer l'historique
        String details = String.format("Produit: %s, Quantité: %s", product.getName(), request.getQuantity());
        recordHistory(invoice, InvoiceHistoryActionType.ADD_LINE, details, principal);

        return toResponse(invoice, salesRepository.findByInvoice_Id(invoiceId));
    }

    /**
     * Méthode de compatibilité pour les appels existants sans principal
     */
    @Transactional
    public InvoiceDTO.InvoiceResponse addLine(UUID invoiceId, InvoiceDTO.AddLineRequest request)
    {
        return addLine(invoiceId, request, null);
    }

    /**
     * Modifie la quantité d'une ligne existante (remplacement, pas addition)
     */
    @Transactional
    public InvoiceDTO.InvoiceResponse modifyLineQuantity(UUID invoiceId, UUID saleId, BigDecimal newQuantity, AuthPrincipal principal)
    {
        InvoiceEntity invoice = getEditableInvoice(invoiceId);

        SaleEntity sale = salesRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable"));

        if (!sale.getInvoice().getId().equals(invoiceId)) {
            throw new IllegalArgumentException("Cette ligne n'appartient pas à cette facture");
        }

        ProductEntity product = sale.getProduct();
        BigDecimal oldQuantity = sale.getQuantity();

        // Vérifier le stock pour la différence
        BigDecimal quantityDiff = newQuantity.subtract(oldQuantity);
        if (quantityDiff.compareTo(BigDecimal.ZERO) > 0) {
            // On augmente la quantité : vérifier le stock disponible
            if (product.getStockQuantity().compareTo(quantityDiff) < 0) {
                throw new IllegalArgumentException("Stock insuffisant pour " + product.getName());
            }
        }

        // Mettre à jour la quantité
        sale.setQuantity(newQuantity);

        // Recalculer les montants de la ligne
        BigDecimal lineSubtotal = product.getSellingPrice().multiply(newQuantity);

        // Règle : si remise FIXED_AMOUNT, elle ne se recalcule PAS automatiquement
        BigDecimal lineDiscount;
        if (sale.getDiscountType() == DiscountTypeEnum.FIXED_AMOUNT && sale.getDiscountValue() != null) {
            // Remise fixe : rester tel quel
            lineDiscount = sale.getDiscountValue();
        } else {
            // Pourcentage ou aucune remise : recalculer
            lineDiscount = computeDiscount(lineSubtotal, sale.getDiscountType() != null ? sale.getDiscountType().name() : null, sale.getDiscountValue());
        }

        BigDecimal lineTotal = lineSubtotal.subtract(lineDiscount);
        BigDecimal lineMargin = product.getSellingPrice().subtract(product.getPurchasePrice())
                .multiply(newQuantity).subtract(lineDiscount);

        sale.setLineSubtotal(lineSubtotal);
        sale.setDiscountAmount(lineDiscount);
        sale.setTotalPrice(lineTotal);
        sale.setMargin(lineMargin);

        salesRepository.save(sale);

        // Mettre à jour le stock produit
        product.setStockQuantity(product.getStockQuantity().subtract(quantityDiff));
        productsRepository.save(product);

        recomputeInvoiceTotals(invoice);

        // Enregistrer l'historique
        String details = String.format("Produit: %s, Quantité: %s -> %s", product.getName(), oldQuantity, newQuantity);
        recordHistory(invoice, InvoiceHistoryActionType.MODIFY_QTY, details, principal);

        return toResponse(invoice, salesRepository.findByInvoice_Id(invoiceId));
    }

    /**
     * Retire une ligne de facture. Si la facture ne contient plus aucune
     * ligne après suppression, elle est elle-même supprimée (une facture
     * à zéro produit n'a pas de sens métier).
     */
    @Transactional
    public InvoiceDTO.InvoiceResponse removeLine(UUID invoiceId, UUID saleId, boolean isManagerAction, AuthPrincipal principal)
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

        // Récupérer la date d'ajout de la ligne (soldAt) pour le recalcul des recettes
        LocalDateTime lineAddedDate = sale.getSoldAt();
        BigDecimal lineTotalPrice = sale.getTotalPrice(); // Montant APRÈS remise

        salesRepository.delete(sale);

        // Enregistrer l'historique
        String details = String.format("Produit: %s, Quantité: %s, Prix après remise: %s", product.getName(), sale.getQuantity(), lineTotalPrice);
        recordHistory(invoice, InvoiceHistoryActionType.REMOVE_LINE, details, principal);

        // Si la facture était validée et payée en espèces, créer un mouvement de remboursement
        if (invoice.getStatus() == InvoiceStatusEnum.VALIDATED && "CASH".equalsIgnoreCase(invoice.getPaymentMethod())) {
            com.sellam.store.cash.services.CashSessionService cashService = org.springframework.web.context.support.WebApplicationContextUtils.getRequiredWebApplicationContext(
                ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()).getRequest().getServletContext()
             ).getBean(com.sellam.store.cash.services.CashSessionService.class);

             // On utilise le "soldBy" de la facture pour trouver l'utilisateur, ou l'utilisateur courant idéalement, mais on n'a pas le principal dans removeLine.
             // On va simuler que le gérant (isManagerAction = true) fait l'action
             UUID personId = UUID.fromString(invoice.getSoldBy());
             cashService.recordRefund(invoice.getShop().getId(), personId, invoiceId, sale.getTotalPrice());
        }

        List<SaleEntity> remainingLines = salesRepository.findByInvoice_Id(invoiceId);

        if (remainingLines.isEmpty())
        {
            invoiceRepository.delete(invoice);
            return toResponse(invoice, List.of());
        }

        recomputeInvoiceTotals(invoice);

        // Recalculer DailyBalanceEntity du jour où la ligne a été ajoutée
        if (lineAddedDate != null) {
            dailyBalanceService.recomputeDailyBalanceForDate(invoice.getShop().getId(), lineAddedDate.toLocalDate());
        }

        return toResponse(invoice, remainingLines);
    }

    /**
     * Méthode de compatibilité pour les appels existants sans principal
     */
    @Transactional
    public InvoiceDTO.InvoiceResponse removeLine(UUID invoiceId, UUID saleId, boolean isManagerAction)
    {
        return removeLine(invoiceId, saleId, isManagerAction, null);
    }

    @Transactional
    public InvoiceDTO.InvoiceResponse applyInvoiceDiscount(UUID invoiceId, InvoiceDTO.ApplyInvoiceDiscountRequest request, AuthPrincipal principal)
    {
        InvoiceEntity invoice = getEditableInvoice(invoiceId);
        invoice.setDiscountType(DiscountTypeEnum.valueOf(request.getDiscountType()));
        invoice.setDiscountValue(request.getDiscountValue());
        recomputeInvoiceTotals(invoice);

        // Enregistrer l'historique
        String details = String.format("Remise globale: %s %s", request.getDiscountType(), request.getDiscountValue());
        recordHistory(invoice, InvoiceHistoryActionType.GLOBAL_DISCOUNT, details, principal);

        return toResponse(invoice, salesRepository.findByInvoice_Id(invoiceId));
    }

    /**
     * Méthode de compatibilité pour les appels existants sans principal
     */
    @Transactional
    public InvoiceDTO.InvoiceResponse applyInvoiceDiscount(UUID invoiceId, InvoiceDTO.ApplyInvoiceDiscountRequest request)
    {
        return applyInvoiceDiscount(invoiceId, request, null);
    }

    /**
     * Applique une remise sur une ligne spécifique avec validation de marge négative
     */
    @Transactional
    public InvoiceDTO.InvoiceResponse applyLineDiscount(UUID invoiceId, UUID saleId, InvoiceDTO.ApplyLineDiscountRequest request, AuthPrincipal principal)
    {
        InvoiceEntity invoice = getEditableInvoice(invoiceId);

        SaleEntity sale = salesRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable"));

        if (!sale.getInvoice().getId().equals(invoiceId)) {
            throw new IllegalArgumentException("Cette ligne n'appartient pas à cette facture");
        }

        ProductEntity product = sale.getProduct();

        // Calculer la nouvelle marge avec la remise
        BigDecimal lineSubtotal = product.getSellingPrice().multiply(sale.getQuantity());
        BigDecimal lineDiscount = computeDiscount(lineSubtotal, request.getDiscountType(), request.getDiscountValue());
        BigDecimal lineTotal = lineSubtotal.subtract(lineDiscount);
        BigDecimal lineMargin = product.getSellingPrice().subtract(product.getPurchasePrice())
                .multiply(sale.getQuantity()).subtract(lineDiscount);

        // Validation : avertissement si marge négative (vente à perte)
        if (lineMargin.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("[APPLY_LINE_DISCOUNT] Avertissement : marge négative détectée pour produit {} (vente à perte)", product.getName());
            // On ne bloque pas, juste un avertissement
        }

        // Mettre à jour la ligne
        sale.setDiscountType(DiscountTypeEnum.valueOf(request.getDiscountType()));
        sale.setDiscountValue(request.getDiscountValue());
        sale.setDiscountAmount(lineDiscount);
        sale.setTotalPrice(lineTotal);
        sale.setMargin(lineMargin);

        salesRepository.save(sale);

        recomputeInvoiceTotals(invoice);

        // Enregistrer l'historique
        String details = String.format("Produit: %s, Remise: %s %s", product.getName(), request.getDiscountType(), request.getDiscountValue());
        recordHistory(invoice, InvoiceHistoryActionType.DISCOUNT, details, principal);

        return toResponse(invoice, salesRepository.findByInvoice_Id(invoiceId));
    }

    @Transactional
    public InvoiceDTO.InvoiceResponse validateInvoice(UUID invoiceId)
    {
        return validateInvoice(invoiceId, null, null);
    }

    @Transactional
    public InvoiceDTO.InvoiceResponse validateInvoice(UUID invoiceId, InvoiceDTO.ValidateInvoiceRequest request, AuthPrincipal principal)
    {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));

        List<SaleEntity> lines = salesRepository.findByInvoice_Id(invoiceId);
        if (lines.isEmpty())
        {
            throw new IllegalArgumentException("Impossible de valider une facture vide");
        }

        if (request != null && request.getCustomerName() != null) {
            String trimmedName = request.getCustomerName().trim();
            invoice.setCustomerName(trimmedName.isEmpty() ? null : trimmedName);
        }

        if (request != null && request.getPaymentMethod() != null) {
            invoice.setPaymentMethod(request.getPaymentMethod());
        } else {
            invoice.setPaymentMethod("CASH"); // Default
        }

        String validatorName = resolveValidatorName(principal);

        invoice.setStatus(InvoiceStatusEnum.VALIDATED);
        invoice.setValidatedByName(validatorName);

        // Ne remplacer soldBy que s'il n'est pas déjà défini (cas de synchronisation hors ligne)
        if (principal != null && invoice.getSoldBy() == null) {
            invoice.setSoldBy(principal.getId().toString());
        }
        
        InvoiceEntity saved = invoiceRepository.save(invoice);
        
        // Enregistrement automatique de la vente en caisse si paiement en espèces
        if ("CASH".equalsIgnoreCase(saved.getPaymentMethod())) {
             com.sellam.store.cash.services.CashSessionService cashService = org.springframework.web.context.support.WebApplicationContextUtils.getRequiredWebApplicationContext(
                ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()).getRequest().getServletContext()
             ).getBean(com.sellam.store.cash.services.CashSessionService.class);
             
             UUID personId = principal != null ? principal.getId() : saved.getShop().getId(); // fallback si pas de principal
             if (principal != null) {
                 cashService.recordCashSale(saved.getShop().getId(), personId, saved);
             }
        }
        
        return toResponse(saved, lines);
    }

    /**
     * Résout le nom à afficher pour le validateur de la facture.
     */
    private String resolveValidatorName(AuthPrincipal principal)
    {
        if (principal == null) {
            return "Sync";
        }
        return personRepository.findById(principal.getId())
                .map(PersonEntity::getName)
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
                .paymentMethod(invoice.getPaymentMethod())
                .soldBy(invoice.getSoldBy())
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

    /**
     * Enregistre une entrée d'historique pour une modification de facture
     */
    private void recordHistory(InvoiceEntity invoice, InvoiceHistoryActionType actionType, String details, AuthPrincipal principal)
    {
        String actorName = resolveValidatorName(principal);
        String actorId = principal != null ? principal.getId().toString() : null;

        InvoiceHistoryEntity history = InvoiceHistoryEntity.builder()
                .invoice(invoice)
                .actionType(actionType)
                .actorName(actorName)
                .actorId(actorId)
                .details(details)
                .build();

        invoiceHistoryRepository.save(history);
    }

}