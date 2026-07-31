package com.sellam.store.dailybalance.controllers;

import com.sellam.store.dailybalance.dto.DailyBalanceDTO;
import com.sellam.store.dailybalance.services.DailyBalanceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/daily-balance")
public class DailyBalanceRestController
{

    private final DailyBalanceService dailyBalanceService;

    public DailyBalanceRestController(DailyBalanceService dailyBalanceService)
    {
        this.dailyBalanceService = dailyBalanceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public DailyBalanceDTO.DailyBalanceResponse declareCash
            (
            @RequestParam UUID shopId,
            @RequestBody DailyBalanceDTO.DeclareCashRequest request
            )
    {
        return dailyBalanceService.declareCash(shopId, request);

    }


    @GetMapping("/history")
    @ResponseStatus(HttpStatus.OK)
    public List<DailyBalanceDTO.DailyBalanceResponse> listHistory(@RequestParam UUID shopId)
    {
        return dailyBalanceService.listHistory(shopId);

    }
}