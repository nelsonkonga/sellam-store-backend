package com.sellam.store.reports.controllers;

import com.sellam.store.reports.dto.ReportsDTO;
import com.sellam.store.reports.services.ReportsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportsRestController {

    private final ReportsService reportsService;

    public ReportsRestController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ReportsDTO.SummaryReport> getSummary(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(reportsService.getSummaryReport(shopId, start, end));
    }

    @GetMapping("/products")
    public ResponseEntity<List<ReportsDTO.TopProductItem>> getProductPerformance(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "volume") String sortBy) {
        return ResponseEntity.ok(reportsService.getProductPerformance(shopId, start, end, sortBy));
    }

    @GetMapping("/cash-reliability")
    public ResponseEntity<List<ReportsDTO.CashReliabilityItem>> getCashReliability(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(reportsService.getCashReliability(shopId, start, end));
    }

    @GetMapping("/negative-margins")
    public ResponseEntity<List<ReportsDTO.NegativeMarginSaleItem>> getNegativeMargins(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(reportsService.getNegativeMarginSales(shopId, start, end));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<ReportsDTO.CategoryPerformanceItem>> getCategoryPerformance(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(reportsService.getCategoryPerformance(shopId, start, end));
    }

    @GetMapping("/dead-stock")
    public ResponseEntity<List<ReportsDTO.DeadStockItem>> getDeadStock(@RequestParam UUID shopId) {
        return ResponseEntity.ok(reportsService.getDeadStock(shopId));
    }

    @GetMapping("/payment-methods")
    public ResponseEntity<List<ReportsDTO.PaymentMethodItem>> getPaymentMethodPerformance(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(reportsService.getPaymentMethodPerformance(shopId, start, end));
    }

    @GetMapping("/employees")
    public ResponseEntity<List<ReportsDTO.EmployeePerformanceItem>> getEmployeePerformance(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(reportsService.getEmployeePerformance(shopId, start, end));
    }

    @GetMapping("/customers")
    public ResponseEntity<List<ReportsDTO.CustomerPerformanceItem>> getCustomerPerformance(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(reportsService.getCustomerPerformance(shopId, start, end));
    }

    // --- VAGUE 3 ---

    @GetMapping("/cashflow-projection")
    public ResponseEntity<ReportsDTO.CashflowProjection> getCashflowProjection(@RequestParam UUID shopId) {
        return ResponseEntity.ok(reportsService.getCashflowProjection(shopId));
    }

    @GetMapping("/dormant-stock-value")
    public ResponseEntity<ReportsDTO.DormantStockValue> getDormantStockValue(@RequestParam UUID shopId) {
        return ResponseEntity.ok(reportsService.getDormantStockValue(shopId));
    }

    @GetMapping("/stock-out-alerts")
    public ResponseEntity<List<ReportsDTO.StockOutAlertItem>> getStockOutAlerts(@RequestParam UUID shopId) {
        return ResponseEntity.ok(reportsService.getStockOutAlerts(shopId));
    }

    @GetMapping("/bought-together")
    public ResponseEntity<List<ReportsDTO.FrequentlyBoughtTogetherItem>> getFrequentlyBoughtTogether(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(reportsService.getFrequentlyBoughtTogether(shopId, start, end));
    }

    @GetMapping("/anomalies")
    public ResponseEntity<List<ReportsDTO.SalesAnomalyItem>> getSalesAnomalies(
            @RequestParam UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(reportsService.getSalesAnomalies(shopId, start, end));
    }
}
