package com.sellam.store.sales.controllers;

import com.sellam.store.sales.dto.SaleDTO;
import com.sellam.store.sales.services.SalesService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sales")
public class SalesRestController
{

    private final SalesService salesService;

    public SalesRestController(SalesService salesService
    )
    {
        this.salesService = salesService;
    }

    // La création de ventes passe désormais exclusivement par les factures
    // (POST /api/invoices/{id}/lines). Ce contrôleur ne conserve que les endpoints
    // de LECTURE utilisés par le dashboard.


    @GetMapping("/today")
    @ResponseStatus(HttpStatus.OK)
    public List<SaleDTO.SaleResponse> listTodaySales(@RequestParam UUID shopId)
    {
        return salesService.listTodaySales(shopId);
    }

    @GetMapping("/shop/{shopId}")
    @ResponseStatus(HttpStatus.OK)
    public List<SaleDTO.SaleResponse> listSalesByPeriod(@PathVariable UUID shopId, @RequestParam(defaultValue = "recent") String period)
    {
        return salesService.listSalesByPeriod(shopId, period);
    }
}