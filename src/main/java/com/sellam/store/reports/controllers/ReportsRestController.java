package com.sellam.store.reports.controllers;

import com.sellam.store.common.email.services.EmailService;
import com.sellam.store.common.security.IShopAccessGuard;
import com.sellam.store.reports.dto.ReportsDTO;
import com.sellam.store.reports.services.ReportsPdfService;
import com.sellam.store.reports.services.ReportsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportsRestController {

    private final ReportsService reportsService;
    private final ReportsPdfService reportsPdfService;
    private final EmailService emailService;
    private final IShopAccessGuard shopAccessGuard;

    public ReportsRestController(ReportsService reportsService, ReportsPdfService reportsPdfService, EmailService emailService, IShopAccessGuard shopAccessGuard) {
        this.reportsService = reportsService;
        this.reportsPdfService = reportsPdfService;
        this.emailService = emailService;
        this.shopAccessGuard = shopAccessGuard;
    }

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportsDTO.SummaryReport> getSummary(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        return ResponseEntity.ok(reportsService.getSummaryReport(shopId, start, end));
    }

    @GetMapping("/products")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportsDTO.TopProductItem>> getProductPerformance(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "volume") String sortBy,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        return ResponseEntity.ok(reportsService.getProductPerformance(shopId, start, end, sortBy));
    }

    @GetMapping("/cash-reliability")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportsDTO.CashReliabilityItem>> getCashReliability(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        return ResponseEntity.ok(reportsService.getCashReliability(shopId, start, end));
    }

    @GetMapping("/negative-margins")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportsDTO.NegativeMarginSaleItem>> getNegativeMargins(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        return ResponseEntity.ok(reportsService.getNegativeMarginSales(shopId, start, end));
    }

    @GetMapping("/categories")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportsDTO.CategoryPerformanceItem>> getCategoryPerformance(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        return ResponseEntity.ok(reportsService.getCategoryPerformance(shopId, start, end));
    }

    @GetMapping("/dead-stock")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportsDTO.DeadStockItem>> getDeadStock(@RequestParam UUID shopId, Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        return ResponseEntity.ok(reportsService.getDeadStock(shopId));
    }

    @GetMapping("/payment-methods")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportsDTO.PaymentMethodItem>> getPaymentMethodPerformance(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        return ResponseEntity.ok(reportsService.getPaymentMethodPerformance(shopId, start, end));
    }

    @GetMapping("/employees")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportsDTO.EmployeePerformanceItem>> getEmployeePerformance(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        return ResponseEntity.ok(reportsService.getEmployeePerformance(shopId, start, end));
    }

    @GetMapping("/customers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportsDTO.CustomerPerformanceItem>> getCustomerPerformance(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        return ResponseEntity.ok(reportsService.getCustomerPerformance(shopId, start, end));
    }

    // --- VAGUE 3 ---

    @GetMapping("/cashflow-projection")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportsDTO.CashflowProjection> getCashflowProjection(@RequestParam UUID shopId, Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        return ResponseEntity.ok(reportsService.getCashflowProjection(shopId));
    }

    @GetMapping("/dormant-stock-value")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportsDTO.DormantStockValue> getDormantStockValue(@RequestParam UUID shopId, Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        return ResponseEntity.ok(reportsService.getDormantStockValue(shopId));
    }

    @GetMapping("/stock-out-alerts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportsDTO.StockOutAlertItem>> getStockOutAlerts(@RequestParam UUID shopId, Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        return ResponseEntity.ok(reportsService.getStockOutAlerts(shopId));
    }

    @GetMapping("/bought-together")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportsDTO.FrequentlyBoughtTogetherItem>> getFrequentlyBoughtTogether(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        return ResponseEntity.ok(reportsService.getFrequentlyBoughtTogether(shopId, start, end));
    }

    @GetMapping("/anomalies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportsDTO.SalesAnomalyItem>> getSalesAnomalies(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        return ResponseEntity.ok(reportsService.getSalesAnomalies(shopId, start, end));
    }

    // --- PDF EXPORT ENDPOINTS ---

    @GetMapping("/summary/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportSummaryPdf(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        ReportsDTO.SummaryReport report = reportsService.getSummaryReport(shopId, start, end);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateSummaryReportPdf(shopName, start, end, report);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rapport_synthese.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/products/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportProductsPdf(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "volume") String sortBy,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.TopProductItem> items = reportsService.getProductPerformance(shopId, start, end, sortBy);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateProductPerformancePdf(shopName, start, end, items);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=performance_produits.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/categories/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportCategoriesPdf(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.CategoryPerformanceItem> items = reportsService.getCategoryPerformance(shopId, start, end);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateCategoryPerformancePdf(shopName, start, end, items);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=performance_categories.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/employees/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportEmployeesPdf(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.EmployeePerformanceItem> items = reportsService.getEmployeePerformance(shopId, start, end);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateEmployeePerformancePdf(shopName, start, end, items);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=performance_employes.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/cash-reliability/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportCashReliabilityPdf(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.CashReliabilityItem> items = reportsService.getCashReliability(shopId, start, end);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateCashReliabilityPdf(shopName, start, end, items);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fiabilite_caisse.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/negative-margins/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportNegativeMarginsPdf(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.NegativeMarginSaleItem> items = reportsService.getNegativeMarginSales(shopId, start, end);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateNegativeMarginSalesPdf(shopName, start, end, items);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ventes_marge_negative.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/dead-stock/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportDeadStockPdf(@RequestParam UUID shopId, Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.DeadStockItem> items = reportsService.getDeadStock(shopId);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateDeadStockPdf(shopName, items);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=stock_mort.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/payment-methods/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportPaymentMethodsPdf(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.PaymentMethodItem> items = reportsService.getPaymentMethodPerformance(shopId, start, end);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generatePaymentMethodPerformancePdf(shopName, start, end, items);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=moyens_paiement.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/customers/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportCustomersPdf(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.CustomerPerformanceItem> items = reportsService.getCustomerPerformance(shopId, start, end);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateCustomerPerformancePdf(shopName, start, end, items);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=performance_clients.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/cashflow-projection/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportCashflowProjectionPdf(@RequestParam UUID shopId, Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        ReportsDTO.CashflowProjection projection = reportsService.getCashflowProjection(shopId);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateCashflowProjectionPdf(shopName, projection);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=projection_tresorerie.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/dormant-stock-value/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportDormantStockValuePdf(@RequestParam UUID shopId, Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        ReportsDTO.DormantStockValue value = reportsService.getDormantStockValue(shopId);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateDormantStockValuePdf(shopName, value);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=valeur_stock_dormant.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/stock-out-alerts/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportStockOutAlertsPdf(@RequestParam UUID shopId, Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.StockOutAlertItem> items = reportsService.getStockOutAlerts(shopId);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateStockOutAlertsPdf(shopName, items);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=alertes_rupture_stock.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/bought-together/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportBoughtTogetherPdf(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.FrequentlyBoughtTogetherItem> items = reportsService.getFrequentlyBoughtTogether(shopId, start, end);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateFrequentlyBoughtTogetherPdf(shopName, start, end, items);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=produits_achetes_ensemble.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/anomalies/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportAnomaliesPdf(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.SalesAnomalyItem> items = reportsService.getSalesAnomalies(shopId, start, end);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateSalesAnomaliesPdf(shopName, start, end, items);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=anomalies_ventes.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // --- EMAIL SHARING ENDPOINTS ---

    @PostMapping("/summary/email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> emailSummary(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam String toEmail,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        ReportsDTO.SummaryReport report = reportsService.getSummaryReport(shopId, start, end);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateSummaryReportPdf(shopName, start, end, report);
        emailService.sendReportByEmail(toEmail, "Rapport Synthétique", pdf);
        return ResponseEntity.ok("Rapport envoyé par email");
    }

    @PostMapping("/products/email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> emailProducts(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam String toEmail,
            @RequestParam(defaultValue = "volume") String sortBy,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.TopProductItem> items = reportsService.getProductPerformance(shopId, start, end, sortBy);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateProductPerformancePdf(shopName, start, end, items);
        emailService.sendReportByEmail(toEmail, "Performance des Produits", pdf);
        return ResponseEntity.ok("Rapport envoyé par email");
    }

    @PostMapping("/categories/email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> emailCategories(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam String toEmail,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.CategoryPerformanceItem> items = reportsService.getCategoryPerformance(shopId, start, end);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateCategoryPerformancePdf(shopName, start, end, items);
        emailService.sendReportByEmail(toEmail, "Performance par Catégorie", pdf);
        return ResponseEntity.ok("Rapport envoyé par email");
    }

    @PostMapping("/employees/email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> emailEmployees(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam String toEmail,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.EmployeePerformanceItem> items = reportsService.getEmployeePerformance(shopId, start, end);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateEmployeePerformancePdf(shopName, start, end, items);
        emailService.sendReportByEmail(toEmail, "Performance des Employés", pdf);
        return ResponseEntity.ok("Rapport envoyé par email");
    }

    @PostMapping("/cash-reliability/email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> emailCashReliability(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam String toEmail,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.CashReliabilityItem> items = reportsService.getCashReliability(shopId, start, end);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateCashReliabilityPdf(shopName, start, end, items);
        emailService.sendReportByEmail(toEmail, "Fiabilité de Caisse", pdf);
        return ResponseEntity.ok("Rapport envoyé par email");
    }

    @PostMapping("/negative-margins/email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> emailNegativeMargins(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam String toEmail,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.NegativeMarginSaleItem> items = reportsService.getNegativeMarginSales(shopId, start, end);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateNegativeMarginSalesPdf(shopName, start, end, items);
        emailService.sendReportByEmail(toEmail, "Ventes à Marge Négative", pdf);
        return ResponseEntity.ok("Rapport envoyé par email");
    }

    @PostMapping("/dead-stock/email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> emailDeadStock(
            @RequestParam UUID shopId,
            @RequestParam String toEmail,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.DeadStockItem> items = reportsService.getDeadStock(shopId);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateDeadStockPdf(shopName, items);
        emailService.sendReportByEmail(toEmail, "Stock Mort", pdf);
        return ResponseEntity.ok("Rapport envoyé par email");
    }

    @PostMapping("/payment-methods/email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> emailPaymentMethods(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam String toEmail,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.PaymentMethodItem> items = reportsService.getPaymentMethodPerformance(shopId, start, end);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generatePaymentMethodPerformancePdf(shopName, start, end, items);
        emailService.sendReportByEmail(toEmail, "Performance par Moyen de Paiement", pdf);
        return ResponseEntity.ok("Rapport envoyé par email");
    }

    @PostMapping("/customers/email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> emailCustomers(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam String toEmail,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.CustomerPerformanceItem> items = reportsService.getCustomerPerformance(shopId, start, end);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateCustomerPerformancePdf(shopName, start, end, items);
        emailService.sendReportByEmail(toEmail, "Performance Clients", pdf);
        return ResponseEntity.ok("Rapport envoyé par email");
    }

    @PostMapping("/cashflow-projection/email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> emailCashflowProjection(
            @RequestParam UUID shopId,
            @RequestParam String toEmail,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        ReportsDTO.CashflowProjection projection = reportsService.getCashflowProjection(shopId);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateCashflowProjectionPdf(shopName, projection);
        emailService.sendReportByEmail(toEmail, "Projection de Trésorerie", pdf);
        return ResponseEntity.ok("Rapport envoyé par email");
    }

    @PostMapping("/dormant-stock-value/email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> emailDormantStockValue(
            @RequestParam UUID shopId,
            @RequestParam String toEmail,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        ReportsDTO.DormantStockValue value = reportsService.getDormantStockValue(shopId);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateDormantStockValuePdf(shopName, value);
        emailService.sendReportByEmail(toEmail, "Valeur du Stock Dormant", pdf);
        return ResponseEntity.ok("Rapport envoyé par email");
    }

    @PostMapping("/stock-out-alerts/email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> emailStockOutAlerts(
            @RequestParam UUID shopId,
            @RequestParam String toEmail,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.StockOutAlertItem> items = reportsService.getStockOutAlerts(shopId);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateStockOutAlertsPdf(shopName, items);
        emailService.sendReportByEmail(toEmail, "Alertes de Rupture de Stock", pdf);
        return ResponseEntity.ok("Rapport envoyé par email");
    }

    @PostMapping("/bought-together/email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> emailBoughtTogether(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam String toEmail,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.FrequentlyBoughtTogetherItem> items = reportsService.getFrequentlyBoughtTogether(shopId, start, end);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateFrequentlyBoughtTogetherPdf(shopName, start, end, items);
        emailService.sendReportByEmail(toEmail, "Produits Fréquemment Achetés Ensemble", pdf);
        return ResponseEntity.ok("Rapport envoyé par email");
    }

    @PostMapping("/anomalies/email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> emailAnomalies(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam String toEmail,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        List<ReportsDTO.SalesAnomalyItem> items = reportsService.getSalesAnomalies(shopId, start, end);
        String shopName = reportsService.getShopName(shopId);
        byte[] pdf = reportsPdfService.generateSalesAnomaliesPdf(shopName, start, end, items);
        emailService.sendReportByEmail(toEmail, "Anomalies de Ventes", pdf);
        return ResponseEntity.ok("Rapport envoyé par email");
    }
}
