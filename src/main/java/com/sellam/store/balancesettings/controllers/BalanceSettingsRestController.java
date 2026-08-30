package com.sellam.store.balancesettings.controllers;

import com.sellam.store.balancesettings.dto.BalanceSettingsDTO;
import com.sellam.store.balancesettings.services.BalanceSettingsService;
import com.sellam.store.common.security.IShopAccessGuard;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/balance-settings")
public class BalanceSettingsRestController
{

    private final BalanceSettingsService balanceSettingsService;
    private final IShopAccessGuard shopAccessGuard;

    @PreAuthorize("@sec.can(authentication, 'MANAGE_DAILY_BALANCE')")
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public BalanceSettingsDTO.SettingsResponse saveSetting(
            @RequestParam UUID shopId,
            @RequestBody BalanceSettingsDTO.SettingsRequest request,
            Authentication authentication
    )
    {
        shopAccessGuard.requireShopAccess(authentication, shopId);
        return balanceSettingsService.saveSetting(shopId, request);
    }

    @PreAuthorize("@sec.can(authentication, 'VIEW_DAILY_BALANCE')")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<BalanceSettingsDTO.SettingsResponse> listSettings(
            @RequestParam UUID shopId,
            Authentication authentication
    )
    {
        shopAccessGuard.requireShopAccess(authentication, shopId);
        return balanceSettingsService.listSettings(shopId);
    }
}