package com.sellam.store.shops.controllers;

import com.sellam.store.shops.dto.ShopDTO;
import com.sellam.store.shops.services.ShopService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shops")
public class ShopController
{

    private final ShopService shopService;

    public ShopController(ShopService shopService)
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
        UUID accountId = (UUID) authentication.getPrincipal();
        return shopService.createShop(request, accountId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ShopDTO.ShopResponse> listShops(Authentication authentication)
    {
        UUID accountId = (UUID) authentication.getPrincipal();
        return shopService.listShops(accountId);
    }
}