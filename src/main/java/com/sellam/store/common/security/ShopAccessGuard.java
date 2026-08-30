package com.sellam.store.common.security;

import com.sellam.store.shops.repositories.ShopRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.context.annotation.Profile;

import java.util.UUID;

/**
 * Centralise la vérification d'accès à une boutique donnée, pour éviter
 * la duplication du même bloc dans chaque contrôleur.

 * - ACCOUNT : la boutique doit lui appartenir (ShopEntity.account.id) —
 *   nécessite ShopRepository, pas principal.getShopId() qui est toujours
 *   null pour un ACCOUNT (voir AuthPrincipal).
 * - USER : la boutique doit être SA boutique (principal.getShopId()).
 */
@Component
@Profile("!phase1")
public class ShopAccessGuard implements IShopAccessGuard
{
    private final com.sellam.store.shops.repositories.ShopRepository shopRepository;

    public ShopAccessGuard(com.sellam.store.shops.repositories.ShopRepository shopRepository)
    {
        this.shopRepository = shopRepository;
    }

    /**
     * Extrait et valide le principal depuis l'authentification, ou lève 401.
     */
    public AuthPrincipal requirePrincipal(Authentication authentication)
    {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal))
        {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié");
        }
        return principal;
    }

    /**
     * Vérifie que ce principal a accès à la boutique donnée, ou lève 403.
     * Combine requirePrincipal() + la vérification d'appartenance.
     */
    public AuthPrincipal requireShopAccess(Authentication authentication, UUID shopId)
    {
        AuthPrincipal principal = requirePrincipal(authentication);
        checkShopAccess(principal, shopId);
        return principal;
    }

    /**
     * Vérifie que ce principal (déjà résolu) a accès à la boutique donnée, ou lève 403.
     * À utiliser quand le principal a déjà été extrait par ailleurs (ex: dans
     * une boucle sur plusieurs actions, comme SyncRestController).
     */
    public void checkShopAccess(AuthPrincipal principal, UUID shopId)
    {
        allowed(principal, shopId, shopRepository);
    }

    public static void allowed(AuthPrincipal principal, UUID shopId, ShopRepository shopRepository) {
        boolean allowed;

        if ("ACCOUNT".equals(principal.getUserType()))
        {
            allowed = shopRepository.existsByIdAndAccount_Id(shopId, principal.getId());
        }
        else
        {
            allowed = shopId.equals(principal.getShopId());
        }

        if (!allowed)
        {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé : vous n'avez pas accès à cette boutique");
        }
    }
}