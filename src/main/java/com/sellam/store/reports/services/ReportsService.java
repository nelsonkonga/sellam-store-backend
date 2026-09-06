package com.sellam.store.reports.services;

import com.sellam.store.cash.models.CashRegisterSessionEntity;
import com.sellam.store.cash.models.CashSessionStatusEnum;
import com.sellam.store.cash.repositories.CashRegisterSessionRepository;
import com.sellam.store.invoices.models.InvoiceEntity;
import com.sellam.store.invoices.models.InvoiceStatusEnum;
import com.sellam.store.invoices.repositories.InvoiceRepository;
import com.sellam.store.reports.dto.ReportsDTO;
import com.sellam.store.sales.models.SaleEntity;
import com.sellam.store.sales.repositories.SalesRepository;
import com.sellam.store.shops.repositories.ShopRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReportsService {

    private final InvoiceRepository invoiceRepository;
    private final SalesRepository salesRepository;
    private final CashRegisterSessionRepository cashSessionRepository;
    private final com.sellam.store.products.repositories.ProductsRepository productsRepository;
    private final ShopRepository shopRepository;

    public ReportsService(InvoiceRepository invoiceRepository, SalesRepository salesRepository, CashRegisterSessionRepository cashSessionRepository, com.sellam.store.products.repositories.ProductsRepository productsRepository, ShopRepository shopRepository) {
        this.invoiceRepository = invoiceRepository;
        this.salesRepository = salesRepository;
        this.cashSessionRepository = cashSessionRepository;
        this.productsRepository = productsRepository;
        this.shopRepository = shopRepository;
    }

    public String getShopName(UUID shopId) {
        return shopRepository.findById(shopId)
                .map(shop -> shop.getName() != null ? shop.getName() : "Boutique")
                .orElse("Boutique");
    }

    public ReportsDTO.SummaryReport getSummaryReport(UUID shopId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Object[]> rows = invoiceRepository.getSummaryReportData(shopId, start, end);
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalMargin = BigDecimal.ZERO;
        int count = 0;
        BigDecimal avgBasket = BigDecimal.ZERO;

        if (rows != null && !rows.isEmpty() && rows.get(0) != null) {
            Object[] row = rows.get(0);
            totalRevenue = row[0] != null ? (BigDecimal) row[0] : BigDecimal.ZERO;
            totalMargin = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            count = row[2] != null ? ((Number) row[2]).intValue() : 0;
            if (row[3] != null) {
                avgBasket = (row[3] instanceof BigDecimal)
                        ? (BigDecimal) row[3]
                        : BigDecimal.valueOf(((Number) row[3]).doubleValue()).setScale(2, RoundingMode.HALF_UP);
            }
        }

        return ReportsDTO.SummaryReport.builder()
                .totalRevenue(totalRevenue)
                .totalMargin(totalMargin)
                .invoiceCount(count)
                .averageBasket(avgBasket)
                .build();
    }

    public List<ReportsDTO.TopProductItem> getProductPerformance(UUID shopId, LocalDate startDate, LocalDate endDate, String sortBy) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Object[]> results;
        if ("margin".equalsIgnoreCase(sortBy)) {
            results = salesRepository.getProductPerformanceOrderByMargin(shopId, start, end);
        } else {
            results = salesRepository.getProductPerformanceOrderByQuantity(shopId, start, end);
        }

        return results.stream().map(row -> {
            String name = (String) row[0];
            BigDecimal qty = (BigDecimal) row[1];
            BigDecimal margin = (BigDecimal) row[2];
            return ReportsDTO.TopProductItem.builder()
                    .productName(name)
                    .quantitySold(qty)
                    .marginGenerated(margin != null ? margin : BigDecimal.ZERO)
                    .build();
        }).collect(Collectors.toList());
    }


    public List<ReportsDTO.CashReliabilityItem> getCashReliability(UUID shopId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Object[]> rows = cashSessionRepository.getDailyDiscrepancies(shopId, start, end);

        return rows.stream().map(row -> ReportsDTO.CashReliabilityItem.builder()
                .date((LocalDate) row[0])
                .discrepancy(row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO)
                .build()
        ).collect(Collectors.toList());
    }

    public List<ReportsDTO.NegativeMarginSaleItem> getNegativeMarginSales(UUID shopId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<SaleEntity> sales = salesRepository.findNegativeMarginSales(shopId, start, end);
        return sales.stream().map(s -> ReportsDTO.NegativeMarginSaleItem.builder()
                .saleId(s.getId())
                .productName(s.getProduct().getName())
                .quantity(s.getQuantity())
                .unitSellingPrice(s.getProduct().getSellingPrice())
                .unitPurchasePrice(s.getProduct().getPurchasePrice())
                .margin(s.getMargin())
                .date(s.getSoldAt().toString())
                .build()).collect(Collectors.toList());
    }

    public List<ReportsDTO.CategoryPerformanceItem> getCategoryPerformance(UUID shopId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        
        return salesRepository.getCategoryPerformance(shopId, start, end).stream().map(row ->
            ReportsDTO.CategoryPerformanceItem.builder()
                .category((String) row[0])
                .revenue((BigDecimal) row[1])
                .margin((BigDecimal) row[2])
                .quantitySold((BigDecimal) row[3])
                .build()
        ).collect(Collectors.toList());
    }

    public List<ReportsDTO.DeadStockItem> getDeadStock(UUID shopId) {
        return productsRepository.findDeadStock(shopId).stream().map(row -> {
            String name = (String) row[0];
            BigDecimal stock = (BigDecimal) row[1];
            LocalDateTime lastSale = (LocalDateTime) row[2];
            int days = -1;
            if (lastSale != null) {
                days = (int) java.time.temporal.ChronoUnit.DAYS.between(lastSale, LocalDateTime.now());
            }
            return ReportsDTO.DeadStockItem.builder()
                    .productName(name)
                    .stockQuantity(stock)
                    .daysSinceLastSale(days)
                    .build();
        }).collect(Collectors.toList());
    }

    public List<ReportsDTO.PaymentMethodItem> getPaymentMethodPerformance(UUID shopId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        return invoiceRepository.getPaymentMethodPerformance(shopId, start, end).stream().map(row ->
            ReportsDTO.PaymentMethodItem.builder()
                .method((String) row[0])
                .totalRevenue((BigDecimal) row[1])
                .invoiceCount(((Number) row[2]).intValue())
                .build()
        ).collect(Collectors.toList());
    }

    public List<ReportsDTO.EmployeePerformanceItem> getEmployeePerformance(UUID shopId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        return invoiceRepository.getEmployeePerformance(shopId, start, end).stream().map(row ->
            ReportsDTO.EmployeePerformanceItem.builder()
                .employeeName((String) row[0])
                .totalRevenue((BigDecimal) row[1])
                .invoiceCount(((Number) row[2]).intValue())
                .build()
        ).collect(Collectors.toList());
    }

    public List<ReportsDTO.CustomerPerformanceItem> getCustomerPerformance(UUID shopId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        return invoiceRepository.getCustomerPerformance(shopId, start, end).stream().map(row ->
            ReportsDTO.CustomerPerformanceItem.builder()
                .customerName((String) row[0])
                .totalRevenue((BigDecimal) row[1])
                .invoiceCount(((Number) row[2]).intValue())
                .build()
        ).collect(Collectors.toList());
    }


    // --- VAGUE 3 ---

    /**
     * A5 — Prévision de trésorerie : moyenne mobile pondérée sur 30 jours,
     * les 15 derniers jours pèsent 2× plus que les 15 premiers.
     */
    public ReportsDTO.CashflowProjection getCashflowProjection(UUID shopId) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(30);

        List<Object[]> dailyRevenues = invoiceRepository.getDailyRevenue(shopId, start, end);

        if (dailyRevenues.isEmpty()) {
            return ReportsDTO.CashflowProjection.builder()
                    .past30DaysRevenue(BigDecimal.ZERO)
                    .projectedNext30DaysRevenue(BigDecimal.ZERO)
                    .averageDailyRevenue(BigDecimal.ZERO)
                    .build();
        }

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        LocalDate midPoint = LocalDate.now().minusDays(15);

        for (Object[] row : dailyRevenues) {
            LocalDate day = (LocalDate) row[0];
            BigDecimal dayRevenue = (BigDecimal) row[1];
            totalRevenue = totalRevenue.add(dayRevenue);

            // Les jours récents (< 15 jours) pèsent 2×
            BigDecimal weight = day.isAfter(midPoint) ? BigDecimal.valueOf(2) : BigDecimal.ONE;
            weightedSum = weightedSum.add(dayRevenue.multiply(weight));
            totalWeight = totalWeight.add(weight);
        }

        BigDecimal avgDaily = totalWeight.compareTo(BigDecimal.ZERO) > 0
                ? weightedSum.divide(totalWeight, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal projected = avgDaily.multiply(BigDecimal.valueOf(30));

        return ReportsDTO.CashflowProjection.builder()
                .past30DaysRevenue(totalRevenue)
                .projectedNext30DaysRevenue(projected)
                .averageDailyRevenue(avgDaily)
                .build();
    }

    /**
     * A6 — Valeur du stock dormant : combien d'argent est bloqué
     * dans des produits qui ne bougent pas depuis 30+ jours.
     */
    public ReportsDTO.DormantStockValue getDormantStockValue(UUID shopId) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        List<Object[]> rows = productsRepository.getDormantStockSummary(shopId, cutoffDate);
        int count = 0;
        BigDecimal totalValue = BigDecimal.ZERO;

        if (rows != null && !rows.isEmpty() && rows.get(0) != null) {
            Object[] row = rows.get(0);
            count = row[0] != null ? ((Number) row[0]).intValue() : 0;
            totalValue = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
        }

        return ReportsDTO.DormantStockValue.builder()
                .totalDormantValue(totalValue)
                .dormantProductCount(count)
                .build();
    }

    /**
     * B3 — Alerte rupture prévisionnelle :
     * vitesse d'écoulement sur 30 jours vs stock actuel → jours restants estimés.
     * Alerte si < 7 jours.
     */
    public List<ReportsDTO.StockOutAlertItem> getStockOutAlerts(UUID shopId) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(30);
        long days = 30;

        List<Object[]> velocities = salesRepository.getProductVelocity(shopId, start, end);

        return velocities.stream()
                .map(row -> {
                    String name = (String) row[1];
                    BigDecimal stock = (BigDecimal) row[2];
                    BigDecimal totalSold = (BigDecimal) row[3];
                    BigDecimal dailyVelocity = totalSold.divide(BigDecimal.valueOf(days), 4, RoundingMode.HALF_UP);

                    int estimatedDays = dailyVelocity.compareTo(BigDecimal.ZERO) > 0
                            ? stock.divide(dailyVelocity, 0, RoundingMode.FLOOR).intValue()
                            : 999;

                    return ReportsDTO.StockOutAlertItem.builder()
                            .productName(name)
                            .currentStock(stock)
                            .dailyVelocity(dailyVelocity)
                            .estimatedDaysRemaining(estimatedDays)
                            .build();
                })
                .filter(item -> item.getEstimatedDaysRemaining() <= 7)
                .sorted((a, b) -> Integer.compare(a.getEstimatedDaysRemaining(), b.getEstimatedDaysRemaining()))
                .collect(Collectors.toList());
    }

    /**
     * B5 — Produits fréquemment achetés ensemble (Market Basket Analysis).
     * Paires de produits les plus fréquentes sur la même facture.
     */
    public List<ReportsDTO.FrequentlyBoughtTogetherItem> getFrequentlyBoughtTogether(UUID shopId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        return salesRepository.getFrequentlyBoughtTogether(shopId, start, end).stream()
                .limit(10)
                .map(row -> ReportsDTO.FrequentlyBoughtTogetherItem.builder()
                        .productA((String) row[0])
                        .productB((String) row[1])
                        .pairOccurrences(((Number) row[2]).intValue())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * E1 / E3 — Détection d'anomalies de CA :
     * compare chaque jour à la moyenne ± 2 écarts-types.
     * Si le CA dépasse ou descend au-delà, c'est un pic ou un creux.
     */
    public List<ReportsDTO.SalesAnomalyItem> getSalesAnomalies(UUID shopId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Object[]> dailyRevenues = invoiceRepository.getDailyRevenue(shopId, start, end);
        if (dailyRevenues.size() < 3) return List.of(); // Pas assez de données

        // Calculer moyenne et écart-type
        List<BigDecimal> values = dailyRevenues.stream()
                .map(row -> (BigDecimal) row[1])
                .collect(Collectors.toList());

        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mean = sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);

        BigDecimal varianceSum = values.stream()
                .map(v -> v.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double stdDev = Math.sqrt(varianceSum.divide(BigDecimal.valueOf(values.size()), 10, RoundingMode.HALF_UP).doubleValue());
        BigDecimal stdDevBd = BigDecimal.valueOf(stdDev);

        BigDecimal upperBound = mean.add(stdDevBd.multiply(BigDecimal.valueOf(2)));
        BigDecimal lowerBound = mean.subtract(stdDevBd.multiply(BigDecimal.valueOf(2)));

        List<ReportsDTO.SalesAnomalyItem> anomalies = new java.util.ArrayList<>();
        for (Object[] row : dailyRevenues) {
            LocalDate day = (LocalDate) row[0];
            BigDecimal revenue = (BigDecimal) row[1];

            if (revenue.compareTo(upperBound) > 0) {
                anomalies.add(ReportsDTO.SalesAnomalyItem.builder()
                        .date(day.toString())
                        .revenue(revenue)
                        .averageRevenue(mean)
                        .anomalyType("PIC")
                        .build());
            } else if (revenue.compareTo(lowerBound) < 0) {
                anomalies.add(ReportsDTO.SalesAnomalyItem.builder()
                        .date(day.toString())
                        .revenue(revenue)
                        .averageRevenue(mean)
                        .anomalyType("CREUX")
                        .build());
            }
        }
        return anomalies;
    }
}