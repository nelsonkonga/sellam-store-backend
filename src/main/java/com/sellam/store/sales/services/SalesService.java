package com.sellam.store.sales.services;

import com.sellam.store.sales.dto.SaleDTO;
import com.sellam.store.sales.models.SaleEntity;
import com.sellam.store.sales.repositories.SalesRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service de LECTURE des ventes (dashboard, bilan journalier).
 * La création de ventes passe désormais exclusivement par InvoiceService.addLine().
 */
@Service
public class SalesService
{

    private final SalesRepository salesRepository;

    public SalesService(SalesRepository salesRepository)
    {
        this.salesRepository = salesRepository;
    }

    public List<SaleDTO.SaleResponse> listTodaySales(UUID shopId)
    {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        return salesRepository.findByShop_IdAndSoldAtBetweenOrderBySoldAtDesc(shopId, startOfDay, endOfDay)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<SaleDTO.SaleResponse> listSalesByPeriod(UUID shopId, String period) {
        if ("recent".equalsIgnoreCase(period) || period == null || period.isEmpty()) {
            return salesRepository.findTop5ByShop_IdOrderBySoldAtDesc(shopId)
                    .stream().map(this::toResponse).collect(Collectors.toList());
        }

        LocalDateTime start;
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);

        switch (period.toLowerCase()) {
            case "this_week":
            case "cette_semaine":
                start = LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay();
                break;
            case "this_month":
            case "ce_mois":
                start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                break;
            case "today":
            case "aujourd_hui":
            default:
                start = LocalDate.now().atStartOfDay();
                break;
        }

        return salesRepository.findByShop_IdAndSoldAtBetweenOrderBySoldAtDesc(shopId, start, end)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private SaleDTO.SaleResponse toResponse(SaleEntity sale)
    {
        return SaleDTO.SaleResponse.builder()
                .id(sale.getId())
                .productId(sale.getProduct().getId())
                .productName(sale.getProduct().getName())
                .quantity(sale.getQuantity())
                .totalPrice(sale.getTotalPrice())
                .margin(sale.getMargin())
                .status(sale.getStatus() != null ? sale.getStatus().name() : "CONFIRMED")
                .soldAt(sale.getSoldAt())
                .build();
    }
}