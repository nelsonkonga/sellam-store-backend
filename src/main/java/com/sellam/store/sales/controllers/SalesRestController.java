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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleDTO.SaleResponse registerSale
            (
            @RequestBody SaleDTO.SaleRequest request,
            @RequestParam UUID shopId
            )
    {
        return salesService.registerSale(request, shopId);
    }


    @GetMapping("/today")
    @ResponseStatus(HttpStatus.OK)
    public List<SaleDTO.SaleResponse> listTodaySales(@RequestParam UUID shopId)
    {
        return salesService.listTodaySales(shopId);
    }
}