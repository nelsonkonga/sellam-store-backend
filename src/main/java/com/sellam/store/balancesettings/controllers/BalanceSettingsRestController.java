package com.sellam.store.balancesettings.controllers;

import com.sellam.store.balancesettings.dto.BalanceSettingsDTO;
import com.sellam.store.balancesettings.services.BalanceSettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/balance-settings")
public class BalanceSettingsRestController
{

    private final BalanceSettingsService balanceSettingsService;

    public BalanceSettingsRestController(BalanceSettingsService balanceSettingsService)
    {
        this.balanceSettingsService = balanceSettingsService;
    }


    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public BalanceSettingsDTO.SettingsResponse saveSetting(
            @RequestParam UUID shopId,
            @RequestBody BalanceSettingsDTO.SettingsRequest request
    )
    {
        return balanceSettingsService.saveSetting(shopId, request);
    }


    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<BalanceSettingsDTO.SettingsResponse> listSettings(@RequestParam UUID shopId)
    {
        return balanceSettingsService.listSettings(shopId);
    }
}