package com.sellam.store.shops.controllers;

import com.sellam.store.common.security.AuthPrincipal;
import com.sellam.store.common.security.IShopAccessGuard;
import com.sellam.store.shops.dto.ShopDTO;
import com.sellam.store.shops.services.ShopService;
import com.sellam.store.shops.services.SupabaseStorageService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Créer une boutique et lister ses boutiques restent réservés au ACCOUNT
 * (les employés n'ont pas de notion de "leurs" boutiques, ils appartiennent
 * à UNE boutique). Modifier les paramètres (logo, nom, adresse) est couvert
 * par la permission MANAGE_SHOP_SETTINGS, qu'un ACCOUNT a toujours (bypass)
 * et qu'un employé Manager pourrait avoir en override si le gérant le décide.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/shops")
public class ShopRestController
{

    private final ShopService shopService;
    private final IShopAccessGuard shopAccessGuard;
    private final SupabaseStorageService supabaseStorageService;

    @PreAuthorize("@sec.isAccountOwner(authentication)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShopDTO.ShopResponse createShop
            (
                    @RequestBody ShopDTO.ShopRequest request,
                    Authentication authentication
            )
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);
        return shopService.createShop(request, principal.getId());
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ShopDTO.ShopResponse> listShops(Authentication authentication)
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);

        // Note : on ne fait plus confiance à principal.getShopId() (issu du JWT)
        // pour résoudre les boutiques d'un USER. Ce claim peut devenir obsolète
        // (membership révoquée/supprimée entre-temps) sans que le token expire,
        // ce qui provoquait un IllegalArgumentException sur un id null.
        // On résout systématiquement via les memberships actives en base.
        return shopService.listShops(principal.getId());
    }

    @PreAuthorize("@sec.can(authentication, 'MANAGE_SHOP_SETTINGS')")
    @PatchMapping("/{id}/settings")
    @ResponseStatus(HttpStatus.OK)
    public ShopDTO.ShopResponse updateSettings
            (
                    @PathVariable UUID id,
                    @RequestBody ShopDTO.ShopSettingsRequest request,
                    Authentication authentication
            )
    {
        shopAccessGuard.requireShopAccess(authentication, id);
        return shopService.updateSettings(id, request);
    }

    @PreAuthorize("@sec.can(authentication, 'MANAGE_SHOP_SETTINGS')")
    @PostMapping(value = "/{id}/logo", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> uploadLogo
            (
                    @PathVariable UUID id,
                    @RequestParam("file") MultipartFile file,
                    Authentication authentication
            )
    {
        shopAccessGuard.requireShopAccess(authentication, id);

        String logoUrl = supabaseStorageService.uploadShopLogo(id, file);

        shopService.updateSettings(id, ShopDTO.ShopSettingsRequest.builder()
                .logoUrl(logoUrl)
                .build());

        return Map.of("logoUrl", logoUrl);
    }
}