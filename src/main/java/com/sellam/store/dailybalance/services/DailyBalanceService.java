package com.sellam.store.dailybalance.services;

import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.dailybalance.dto.DailyBalanceDTO;
import com.sellam.store.dailybalance.models.BalanceStatusEnum;
import com.sellam.store.dailybalance.models.DailyBalanceEntity;
import com.sellam.store.dailybalance.repositories.DailyBalanceRepository;
import com.sellam.store.invoices.models.InvoiceEntity;
import com.sellam.store.invoices.models.InvoiceStatusEnum;
import com.sellam.store.invoices.repositories.InvoiceRepository;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DailyBalanceService
{

    private final DailyBalanceRepository dailyBalanceRepository;

    private final InvoiceRepository invoiceRepository;

    private final ShopRepository shopRepository;


    public DailyBalanceService
            (
                    DailyBalanceRepository dailyBalanceRepository,
                    InvoiceRepository invoiceRepository,
                    ShopRepository shopRepository
            )
    {
        this.dailyBalanceRepository = dailyBalanceRepository;
        this.invoiceRepository = invoiceRepository;
        this.shopRepository = shopRepository;
    }


    @Transactional
    public DailyBalanceDTO.DailyBalanceResponse declareCash(UUID shopId, DailyBalanceDTO.DeclareCashRequest request)
    {

        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique introuvable"));

        LocalDate today = LocalDate.now();

        LocalDateTime startOfDay = today.atStartOfDay();

        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<InvoiceEntity> todayInvoices = invoiceRepository.findByShop_IdAndStatusAndCreatedAtBetween(
                shopId, InvoiceStatusEnum.VALIDATED, startOfDay, endOfDay);

        BigDecimal computedTotalSales = todayInvoices.stream()
                .map(InvoiceEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal computedTotalMargin = todayInvoices.stream()
                .map(InvoiceEntity::getTotalMargin)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discrepancy = request.getDeclaredCash().subtract(computedTotalSales);

        BalanceStatusEnum status;
        if (discrepancy.compareTo(BigDecimal.ZERO) == 0)
        {
            status = BalanceStatusEnum.OK;
        }
        else if (discrepancy.compareTo(BigDecimal.ZERO) > 0)
        {
            status = BalanceStatusEnum.POSITIVE_DISCREPANCY;
        }
        else
        {
            status = BalanceStatusEnum.NEGATIVE_DISCREPANCY;
        }


        DailyBalanceEntity balance = dailyBalanceRepository
                .findByShop_IdAndBalanceDate(shopId, today)
                .orElse(new DailyBalanceEntity());

        balance.setShop(shop);
        balance.setBalanceDate(today);
        balance.setComputedTotalSales(computedTotalSales);
        balance.setComputedTotalMargin(computedTotalMargin);
        balance.setDeclaredCash(request.getDeclaredCash());
        balance.setDiscrepancy(discrepancy);
        balance.setStatus(status);

        DailyBalanceEntity saved = dailyBalanceRepository.save(balance);

        return toResponse(saved);
    }


    public List<DailyBalanceDTO.DailyBalanceResponse> listHistory(UUID shopId)
    {
        return dailyBalanceRepository.findByShop_IdOrderByBalanceDateDesc(shopId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


    private DailyBalanceDTO.DailyBalanceResponse toResponse(DailyBalanceEntity balance)
    {
        return DailyBalanceDTO.DailyBalanceResponse.builder()
                .id(balance.getId())
                .balanceDate(balance.getBalanceDate())
                .computedTotalSales(balance.getComputedTotalSales())
                .computedTotalMargin(balance.getComputedTotalMargin())
                .declaredCash(balance.getDeclaredCash())
                .discrepancy(balance.getDiscrepancy())
                .status(balance.getStatus().name())
                .createdAt(balance.getCreatedAt())
                .build();
    }

    /**
     * Recalcul le DailyBalanceEntity pour une date spécifique (utilisé lors de la suppression de lignes)
     * Recalcul les recettes basées sur les factures validées de cette date
     */
    @Transactional
    public void recomputeDailyBalanceForDate(UUID shopId, LocalDate date)
    {
        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique introuvable"));

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<InvoiceEntity> dateInvoices = invoiceRepository.findByShop_IdAndStatusAndCreatedAtBetween(
                shopId, InvoiceStatusEnum.VALIDATED, startOfDay, endOfDay);

        BigDecimal computedTotalSales = dateInvoices.stream()
                .map(InvoiceEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal computedTotalMargin = dateInvoices.stream()
                .map(InvoiceEntity::getTotalMargin)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DailyBalanceEntity balance = dailyBalanceRepository
                .findByShop_IdAndBalanceDate(shopId, date)
                .orElse(new DailyBalanceEntity());

        balance.setShop(shop);
        balance.setBalanceDate(date);
        balance.setComputedTotalSales(computedTotalSales);
        balance.setComputedTotalMargin(computedTotalMargin);

        // Si une déclaration de cash existe, recalculer la différence
        if (balance.getDeclaredCash() != null) {
            BigDecimal discrepancy = balance.getDeclaredCash().subtract(computedTotalSales);
            balance.setDiscrepancy(discrepancy);

            BalanceStatusEnum status;
            if (discrepancy.compareTo(BigDecimal.ZERO) == 0) {
                status = BalanceStatusEnum.OK;
            } else if (discrepancy.compareTo(BigDecimal.ZERO) > 0) {
                status = BalanceStatusEnum.POSITIVE_DISCREPANCY;
            } else {
                status = BalanceStatusEnum.NEGATIVE_DISCREPANCY;
            }
            balance.setStatus(status);
        }

        dailyBalanceRepository.save(balance);
    }
}