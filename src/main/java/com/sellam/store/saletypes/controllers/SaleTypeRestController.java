package com.sellam.store.saletypes.controllers;

import com.sellam.store.saletypes.dto.SaleTypeDTO;
import com.sellam.store.saletypes.services.SaleTypeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sale-types")
public class SaleTypeRestController {

    private final SaleTypeService saleTypeService;

    public SaleTypeRestController(SaleTypeService saleTypeService) {
        this.saleTypeService = saleTypeService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<SaleTypeDTO.Response> listAvailableTypes(@RequestParam UUID shopId) {
        return saleTypeService.listAvailableTypes(shopId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleTypeDTO.Response createCustomType(@RequestParam UUID shopId, @RequestBody SaleTypeDTO.Request request) {
        return saleTypeService.createCustomType(shopId, request);
    }
}
