package com.sellam.store.users.controllers;

import com.sellam.store.common.security.AuthPrincipal;
import com.sellam.store.common.security.IShopAccessGuard;
import com.sellam.store.shops.repositories.ShopRepository;
import com.sellam.store.users.dto.UserDTO;
import com.sellam.store.users.services.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * NOTE : la gestion des employés est un pouvoir réservé exclusivement au
 * propriétaire du compte (ACCOUNT) — non déléguable à un employé (USER),
 * même avec toutes les permissions. D'où @sec.isAccountOwner(authentication)
 * plutôt que @sec.can(authentication, 'X') sur ce contrôleur.

 * checkShopOwnership() complète l'annotation : elle vérifie que la boutique
 * ciblée appartient bien à ce compte, via ShopEntity.account.id — logique
 * spécifique (ACCOUNT uniquement, jamais USER), différente de
 * ShopAccessGuard.checkShopAccess() qui autorise aussi un USER sur SA
 * propre boutique. D'où une méthode dédiée ici plutôt que le composant partagé.
 */
@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping("/api/users")
public class UserRestController
{

    private final UserService userService;
    private final ShopRepository shopRepository;
    private final IShopAccessGuard shopAccessGuard;
    private final com.sellam.store.identity.services.IdentityService identityService;

    @PreAuthorize("@sec.isAccountOwner(authentication)")
    @PostMapping("/shop/{shopId}")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDTO.UserResponse createUser
            (
                    @PathVariable UUID shopId,
                    @RequestBody UserDTO.CreateUserRequest request,
                    Authentication authentication
            )
    {
        checkShopOwnership(authentication, shopId);
        return userService.createUser(shopId, request);
    }

    @PreAuthorize("@sec.isAccountOwner(authentication)")
    @GetMapping("/shop/{shopId}")
    @ResponseStatus(HttpStatus.OK)
    public List<UserDTO.UserResponse> listUsers
            (
                    @PathVariable UUID shopId,
                    Authentication authentication
            )
    {
        checkShopOwnership(authentication, shopId);
        return userService.listUsers(shopId);
    }

    @GetMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public UserDTO.UserResponse getUser(@PathVariable UUID userId, Authentication authentication)
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);
        if (!principal.getId().equals(userId))
        {
            checkShopOwnership(authentication, userService.getShopIdByUserId(userId));
        }
        return userService.getUser(userId);
    }

    @PreAuthorize("@sec.isAccountOwner(authentication)")
    @PutMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public UserDTO.UserResponse updateUser
            (
                    @PathVariable UUID userId,
                    @RequestBody UserDTO.UpdateUserRequest request,
                    Authentication authentication
            )
    {
        UUID resourceShopId = userService.getShopIdByUserId(userId);
        checkShopOwnership(authentication, resourceShopId);
        return userService.updateUser(userId, request);
    }

    @PreAuthorize("@sec.isAccountOwner(authentication)")
    @GetMapping("/{userId}/permissions")
    @ResponseStatus(HttpStatus.OK)
    public UserDTO.PermissionsResponse getPermissions
            (
                    @PathVariable UUID userId,
                    Authentication authentication
            )
    {
        UUID resourceShopId = userService.getShopIdByUserId(userId);
        checkShopOwnership(authentication, resourceShopId);
        return userService.getPermissions(userId);
    }

    @PreAuthorize("@sec.isAccountOwner(authentication)")
    @PutMapping("/{userId}/permissions")
    @ResponseStatus(HttpStatus.OK)
    public UserDTO.PermissionsResponse updatePermissions
            (
                    @PathVariable UUID userId,
                    @RequestBody UserDTO.UpdatePermissionsRequest request,
                    Authentication authentication
            )
    {
        UUID resourceShopId = userService.getShopIdByUserId(userId);
        checkShopOwnership(authentication, resourceShopId);
        return userService.updatePermissions(userId, request);
    }

    @PreAuthorize("@sec.isAccountOwner(authentication)")
    @PatchMapping("/{userId}/password")
    @ResponseStatus(HttpStatus.OK)
    public Void changePassword
            (
                    @PathVariable UUID userId,
                    @RequestBody UserDTO.ChangePasswordRequest request,
                    Authentication authentication
            )
    {
        UUID resourceShopId = userService.getShopIdByUserId(userId);
        checkShopOwnership(authentication, resourceShopId);
        userService.changePassword(userId, request);
        return null;
    }

    @PreAuthorize("@sec.isAccountOwner(authentication)")
    @PatchMapping("/{userId}/toggle-active")
    @ResponseStatus(HttpStatus.OK)
    public UserDTO.UserResponse toggleActive
            (
                    @PathVariable UUID userId,
                    Authentication authentication
            )
    {
        UUID resourceShopId = userService.getShopIdByUserId(userId);
        checkShopOwnership(authentication, resourceShopId);
        return userService.toggleActive(userId);
    }

    @PreAuthorize("@sec.isAccountOwner(authentication)")
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Void deleteUser
            (
                    @PathVariable UUID userId,
                    Authentication authentication
            )
    {
        UUID resourceShopId = userService.getShopIdByUserId(userId);
        checkShopOwnership(authentication, resourceShopId);
        userService.deleteUser(userId);
        return null;
    }

    /**
     * Vérifie que la boutique ciblée appartient bien à ce compte (ACCOUNT).
     * L'authentification et le rôle ACCOUNT sont déjà garantis par
     * {@code @PreAuthorize("@sec.isAccountOwner(...)")} à ce stade — mais on
     * revérifie ici par défense en profondeur.

     * Passe par ShopEntity.account.id, pas par principal.getShopId() qui
     * est toujours null pour un ACCOUNT (voir AuthPrincipal).
     */
    private void checkShopOwnership(Authentication authentication, UUID resourceShopId)
    {
        if (resourceShopId == null)
        {
            return;
        }

        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);

        boolean ownsShop = false;
        
        if ("ACCOUNT".equals(principal.getUserType())) {
            // For backward compatibility: check if a legacy account is tied to this shop via a MANAGER membership
            ownsShop = identityService.hasAccessToShop(principal.getId(), resourceShopId);
        } else {
             ownsShop = identityService.hasAccessToShop(principal.getId(), resourceShopId);
        }
        
        if (!ownsShop)
        {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé : vous n'avez pas accès à cette boutique");
        }
    }
}