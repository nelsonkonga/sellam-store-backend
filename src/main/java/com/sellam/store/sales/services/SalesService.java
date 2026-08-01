package com.sellam.store.sales.services;

import com.sellam.store.products.models.ProductEntity;
import com.sellam.store.products.repositories.ProductsRepository;
import com.sellam.store.sales.dto.SaleDTO;
import com.sellam.store.sales.models.SaleEntity;
import com.sellam.store.sales.models.SaleStatusEnum;
import com.sellam.store.sales.repositories.SalesRepository;
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
public class SalesService
{

    private final SalesRepository salesRepository;

    private final ProductsRepository productsRepository;

    private final ShopRepository shopRepository;


    public SalesService
            (
            SalesRepository salesRepository,
            ProductsRepository productsRepository,
            ShopRepository shopRepository
            )
    {
        this.salesRepository = salesRepository;

        this.productsRepository = productsRepository;

        this.shopRepository = shopRepository;
    }

    @Transactional
    public SaleDTO.SaleResponse registerSale
            (
            SaleDTO.SaleRequest request,
            UUID shopId
            )
    {

        ProductEntity product = productsRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Produit introuvable"));

        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Boutique introuvable"));


        if (product.getStockQuantity().compareTo(request.getQuantity()) < 0) {
            throw new IllegalArgumentException("Stock insuffisant pour ce produit");
        }


        BigDecimal totalPrice = product.getSellingPrice().multiply(request.getQuantity());

        BigDecimal margin = product.getSellingPrice()
                .subtract(product.getPurchasePrice())
                .multiply(request.getQuantity());


        product.setStockQuantity(product.getStockQuantity().subtract(request.getQuantity()));

        productsRepository.save(product);

        SaleEntity sale = SaleEntity.builder()
                                    .product(product)
                                    .shop(shop)
                                    .quantity(request.getQuantity())
                                    .totalPrice(totalPrice)
                                    .margin(margin)
                                    .status(SaleStatusEnum.CONFIRMED)
                                    .build();

        SaleEntity saved = salesRepository.save(sale);

        return toResponse(saved);
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
                .status(sale.getStatus().name())
                .soldAt(sale.getSoldAt())
                .build();
    }
}