package com.sellam.store.sales.controllers;

import com.sellam.store.common.security.IShopAccessGuard;
import com.sellam.store.sales.dto.SaleDTO;
import com.sellam.store.sales.services.SalesService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/sales")
public class SalesRestController
{

    private final SalesService salesService;
    private final IShopAccessGuard shopAccessGuard;

    // Les ventes du jour sont nécessaires au quotidien pour tout employé
    // qui facture (tableau de bord, etc.) — pas de restriction au-delà de l'auth.
    @GetMapping("/today")
    @ResponseStatus(HttpStatus.OK)
    public List<SaleDTO.SaleResponse> listTodaySales(
            @RequestParam UUID shopId,
            Authentication authentication
    )
    {
        shopAccessGuard.requireShopAccess(authentication, shopId);
        return salesService.listTodaySales(shopId);
    }

    @PreAuthorize("@sec.can(authentication, 'VIEW_SALES_HISTORY')")
    @GetMapping("/shop/{shopId}")
    @ResponseStatus(HttpStatus.OK)
    public List<SaleDTO.SaleResponse> listSalesByPeriod(
            @PathVariable UUID shopId,
            @RequestParam(defaultValue = "recent") String period,
            Authentication authentication
    )
    {
        shopAccessGuard.requireShopAccess(authentication, shopId);
        return salesService.listSalesByPeriod(shopId, period);
    }
}