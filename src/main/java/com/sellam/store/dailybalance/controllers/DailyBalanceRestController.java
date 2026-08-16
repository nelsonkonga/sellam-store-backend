package com.sellam.store.dailybalance.controllers;

import com.sellam.store.common.security.ShopAccessGuard;
import com.sellam.store.dailybalance.dto.DailyBalanceDTO;
import com.sellam.store.dailybalance.services.DailyBalanceService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/daily-balance")
public class DailyBalanceRestController
{

    private final DailyBalanceService dailyBalanceService;
    private final ShopAccessGuard shopAccessGuard;

    public DailyBalanceRestController(DailyBalanceService dailyBalanceService, ShopAccessGuard shopAccessGuard)
    {
        this.dailyBalanceService = dailyBalanceService;
        this.shopAccessGuard = shopAccessGuard;
    }

    @PreAuthorize("@sec.can(authentication, 'MANAGE_DAILY_BALANCE')")
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public DailyBalanceDTO.DailyBalanceResponse declareCash
            (
                    @RequestParam UUID shopId,
                    @RequestBody DailyBalanceDTO.DeclareCashRequest request,
                    Authentication authentication
            )
    {
        shopAccessGuard.requireShopAccess(authentication, shopId);
        return dailyBalanceService.declareCash(shopId, request);
    }

    @PreAuthorize("@sec.can(authentication, 'VIEW_DAILY_BALANCE')")
    @GetMapping("/history")
    @ResponseStatus(HttpStatus.OK)
    public List<DailyBalanceDTO.DailyBalanceResponse> listHistory(
            @RequestParam UUID shopId,
            Authentication authentication
    )
    {
        shopAccessGuard.requireShopAccess(authentication, shopId);
        return dailyBalanceService.listHistory(shopId);
    }
}