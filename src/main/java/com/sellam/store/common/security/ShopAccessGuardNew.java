package com.sellam.store.common.security;

import com.sellam.store.identity.repositories.ShopMembershipRepository;
import com.sellam.store.shops.repositories.ShopRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.context.annotation.Profile;

import java.util.UUID;

/**
 * Guard d'accès boutique adapté au nouveau modèle ShopMembershipEntity.
 * 
 * Changements par rapport à l'ancien ShopAccessGuard :
 * - Pour ACCOUNT : vérifie via ShopEntity.account (ancien modèle, compatible)
 * - Pour PERSON : vérifie via ShopMembershipEntity (nouveau modèle multi-boutique)
 * - Pour USER : vérifie via ShopMembershipEntity (ancien UserEntity.shop migré)
 * 
 * Logique :
 * - ACCOUNT/PERSON avec shops propriétaires : shops.account_id == person.id
 * - PERSON/USER avec memberships : membership active pour shop_id
 */
@Component
@Profile("phase1")
public class ShopAccessGuardNew implements IShopAccessGuard
{
    private final ShopRepository shopRepository;
    private final ShopMembershipRepository shopMembershipRepository;

    public ShopAccessGuardNew(ShopRepository shopRepository, ShopMembershipRepository shopMembershipRepository)
    {
        this.shopRepository = shopRepository;
        this.shopMembershipRepository = shopMembershipRepository;
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
     */
    public AuthPrincipal requireShopAccess(Authentication authentication, UUID shopId)
    {
        AuthPrincipal principal = requirePrincipal(authentication);
        checkShopAccess(principal, shopId);
        return principal;
    }

    /**
     * Vérifie que ce principal (déjà résolu) a accès à la boutique donnée, ou lève 403.
     */
    public void checkShopAccess(AuthPrincipal principal, UUID shopId)
    {
        allowed(principal, shopId, shopRepository, shopMembershipRepository);
    }

    public static void allowed(AuthPrincipal principal, UUID shopId,
                               ShopRepository shopRepository,
                               ShopMembershipRepository shopMembershipRepository)
    {
        boolean allowed;

        if ("ACCOUNT".equals(principal.getUserType()))
        {
            // Ancien modèle : vérifier via ShopEntity.account
            allowed = shopRepository.existsByIdAndAccount_Id(shopId, principal.getId());
        }
        else
        {
            // Nouveau modèle : vérifier via ShopMembershipEntity
            allowed = shopMembershipRepository.findActiveMembership(principal.getId(), shopId).isPresent();
        }

        if (!allowed)
        {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé : vous n'avez pas accès à cette boutique");
        }
    }
}
