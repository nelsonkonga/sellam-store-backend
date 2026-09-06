package com.sellam.store.subscriptions.controllers;

import com.sellam.store.common.security.IShopAccessGuard;
import com.sellam.store.subscriptions.dto.SubscriptionDTO;
import com.sellam.store.subscriptions.services.SubscriptionService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/shops/{shopId}/subscription")
public class SubscriptionRestController
{
    private final SubscriptionService subscriptionService;
    private final IShopAccessGuard shopAccessGuard;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public SubscriptionDTO.SubscriptionResponse getSubscription(
            @PathVariable UUID shopId,
            Authentication authentication)
    {
        shopAccessGuard.requireShopAccess(authentication, shopId);
        return subscriptionService.toResponse(subscriptionService.getOrRecalculate(shopId));
    }
}
