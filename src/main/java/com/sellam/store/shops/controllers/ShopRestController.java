package com.sellam.store.shops.controllers;

import com.sellam.store.shops.dto.ShopDTO;
import com.sellam.store.shops.services.ShopService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import com.sellam.store.common.security.AuthPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shops")
public class ShopRestController
{

    private final ShopService shopService;

    public ShopRestController(ShopService shopService)
    {
        this.shopService = shopService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShopDTO.ShopResponse createShop
            (
            @RequestBody ShopDTO.ShopRequest request,
            Authentication authentication
            )
    {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        if ("USER".equals(principal.getUserType())) {
            throw new IllegalArgumentException("Les employés ne peuvent pas créer de boutique");
        }
        return shopService.createShop(request, principal.getId());
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ShopDTO.ShopResponse> listShops(Authentication authentication)
    {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        if ("USER".equals(principal.getUserType())) {
            return List.of(shopService.getShopById(principal.getShopId()));
        }
        return shopService.listShops(principal.getId());
    }


    @PatchMapping("/{id}/settings")
    @ResponseStatus(HttpStatus.OK)
    public ShopDTO.ShopResponse updateSettings(
            @PathVariable UUID id,
            @RequestBody ShopDTO.ShopSettingsRequest request
    )
    {
        return shopService.updateSettings(id, request);
    }
}