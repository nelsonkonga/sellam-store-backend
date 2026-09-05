package com.sellam.store.saletypes.controllers;

import com.sellam.store.common.security.IShopAccessGuard;
import com.sellam.store.saletypes.dto.SaleTypeDTO;
import com.sellam.store.saletypes.services.SaleTypeService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/sale-types")
public class SaleTypeRestController
{

    private final SaleTypeService saleTypeService;
    private final IShopAccessGuard shopAccessGuard;

    // Lecture des types de vente disponibles : nécessaire à tout employé
    // qui facture, donc pas de restriction de permission au-delà de l'auth.
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<SaleTypeDTO.SaleTypeResponse> listAvailableTypes(
            @RequestParam UUID shopId,
            Authentication authentication
    )
    {
        shopAccessGuard.requireShopAccess(authentication, shopId);
        return saleTypeService.listAvailableTypes(shopId);
    }

    @PreAuthorize("@sec.can(authentication, 'MANAGE_SALE_TYPES')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleTypeDTO.SaleTypeResponse createCustomType(
            @RequestParam UUID shopId,
            @RequestBody SaleTypeDTO.SaleTypeRequest saleTypeRequest,
            Authentication authentication
    )
    {
        shopAccessGuard.requireShopAccess(authentication, shopId);
        return saleTypeService.createCustomType(shopId, saleTypeRequest);
    }
}