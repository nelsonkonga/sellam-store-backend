package com.sellam.store.common.security;

import com.sellam.store.identity.repositories.ShopMembershipRepository;
import com.sellam.store.shops.repositories.ShopRepository;
import com.sellam.store.users.models.PermissionEnum;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;


import java.util.UUID;

/**
 * Guard d'accÃ¨s boutique adaptÃ© au nouveau modÃ¨le ShopMembershipEntity.
 * 
 * Changements par rapport Ã  l'ancien ShopAccessGuard :
 * - Pour ACCOUNT : vÃ©rifie via ShopEntity.account (ancien modÃ¨le, compatible)
 * - Pour PERSON : vÃ©rifie via ShopMembershipEntity (nouveau modÃ¨le multi-boutique)
 * - Pour USER : vÃ©rifie via ShopMembershipEntity (ancien UserEntity.shop migrÃ©)
 * 
 * Logique :
 * - ACCOUNT/PERSON avec shops propriÃ©taires : shops.account_id == person.id
 * - PERSON/USER avec memberships : membership active pour shop_id
 */
@Component

public class ShopAccessGuard implements IShopAccessGuard
{
    private final ShopRepository shopRepository;
    private final ShopMembershipRepository shopMembershipRepository;

    public ShopAccessGuard(ShopRepository shopRepository, ShopMembershipRepository shopMembershipRepository)
    {
        this.shopRepository = shopRepository;
        this.shopMembershipRepository = shopMembershipRepository;
    }

    /**
     * Extrait et valide le principal depuis l'authentification, ou lÃ¨ve 401.
     */
    public AuthPrincipal requirePrincipal(Authentication authentication)
    {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal))
        {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifiÃ©");
        }
        return principal;
    }

    /**
     * VÃ©rifie que ce principal a accÃ¨s Ã  la boutique donnÃ©e, ou lÃ¨ve 403.
     */
    public AuthPrincipal requireShopAccess(Authentication authentication, UUID shopId)
    {
        AuthPrincipal principal = requirePrincipal(authentication);
        checkShopAccess(principal, shopId);
        return principal;
    }

    /**
     * VÃ©rifie que ce principal (dÃ©jÃ  rÃ©solu) a accÃ¨s Ã  la boutique donnÃ©e, ou lÃ¨ve 403.
     */
    public void checkShopAccess(AuthPrincipal principal, UUID shopId)
    {
        allowed(principal, shopId, shopRepository, shopMembershipRepository);
    }

    public void checkShopAccess(Authentication authentication, UUID shopId)
    {
        AuthPrincipal principal = requirePrincipal(authentication);
        checkShopAccess(principal, shopId);
    }

    public void requirePermission(Authentication authentication, UUID shopId, PermissionEnum permission)
    {
        AuthPrincipal principal = requirePrincipal(authentication);
        checkShopAccess(principal, shopId);
        if ("ACCOUNT".equals(principal.getUserType()))
        {
            return;
        }
        boolean allowed = shopMembershipRepository.findActiveMembership(principal.getId(), shopId)
                .map(membership -> membership.getEffectivePermissions().contains(permission))
                .orElse(false);
        if (!allowed)
        {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'avez pas le droit d'accéder aux rapports de cette boutique.");
        }
    }

    public static void allowed(AuthPrincipal principal, UUID shopId,
                               ShopRepository shopRepository,
                               ShopMembershipRepository shopMembershipRepository)
    {
        // Modèle unifié PersonEntity+ShopMembership :
        // ACCOUNT et USER sont désormais des PersonEntity avec (ou sans) ShopMembershipEntity.
        // On vérifie toujours via ShopMembershipRepository.
        boolean allowed = shopMembershipRepository.findActiveMembership(principal.getId(), shopId).isPresent();

        if (!allowed)
        {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé : vous n'avez pas accès à cette boutique");
        }
    }
}
