package com.sellam.store.identity.controllers;

import com.sellam.store.common.security.AuthPrincipal;
import com.sellam.store.common.security.IShopAccessGuard;
import com.sellam.store.identity.dto.IdentityDTO;
import com.sellam.store.identity.models.ShopMembershipEntity;
import com.sellam.store.identity.repositories.ShopMembershipRepository;
import com.sellam.store.identity.services.IdentityService;
import com.sellam.store.users.models.PermissionEnum;
import com.sellam.store.users.models.RoleEnum;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ContrÃ´leur pour la gestion de l'identitÃ© et des memberships multi-boutique.
 * 
 * Endpoints :
 * - Gestion des memberships (crÃ©ation, modification, suppression)
 * - Changement de contact (phone/email) avec validation des 3 mois
 * - Liste des boutiques accessibles
 * - SÃ©lection de boutique active
 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/identity")

public class IdentityController
{

    private final IdentityService identityService;
    private final IShopAccessGuard shopAccessGuard;
    private final ShopMembershipRepository shopMembershipRepository;

    /**
     * RÃ©cupÃ¨re toutes les memberships de la personne authentifiÃ©e.
     */
    @GetMapping("/memberships")
    @ResponseStatus(HttpStatus.OK)
    public List<IdentityDTO.MembershipResponse> getMyMemberships(Authentication authentication)
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);
        List<ShopMembershipEntity> memberships = identityService.getActiveMemberships(principal.getId());

        return memberships.stream()
                .map(this::toMembershipResponse)
                .collect(Collectors.toList());
    }

    /**
     * RÃ©cupÃ¨re la membership pour une boutique spÃ©cifique.
     */
    @GetMapping("/memberships/shop/{shopId}")
    @ResponseStatus(HttpStatus.OK)
    public IdentityDTO.MembershipResponse getMembership(
            @PathVariable UUID shopId,
            Authentication authentication)
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);
        ShopMembershipEntity membership = identityService.getMembership(principal.getId(), shopId);
        return toMembershipResponse(membership);
    }

    /**
     * CrÃ©e une nouvelle membership (rÃ©servÃ© aux gÃ©rants de boutique).
     *
     * RÃ¨gles d'accÃ¨s :
     * - ACCOUNT (ancien modÃ¨le) : peut crÃ©er dans ses boutiques via ShopEntity.account
     * - PERSON (nouveau modÃ¨le) : doit Ãªtre MANAGER dans la boutique cible
     *
     * NOTE : Le check est strict sur le rÃ´le MANAGER, pas sur la permission effective.
     * Un CASHIER avec MANAGE_SHOP_SETTINGS via override NE PEUT PAS crÃ©er de memberships.
     * Cette restriction est intentionnelle pour la sensibilitÃ© de l'action.
     */
    @PreAuthorize("@sec.isAccountOwner(authentication) || @sec.can(authentication, 'MANAGE_SHOP_SETTINGS')")
    @PostMapping("/memberships")
    @ResponseStatus(HttpStatus.CREATED)
    public IdentityDTO.MembershipResponse createMembership(
            @RequestBody IdentityDTO.CreateMembershipRequest request,
            Authentication authentication)
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);

        // Pour ACCOUNT : vÃ©rifier via ShopEntity.account (boutiques possÃ©dÃ©es)
        if ("ACCOUNT".equals(principal.getUserType()))
        {
            shopAccessGuard.checkShopAccess(principal, request.getShopId());
        }
        // Pour PERSON : vÃ©rifier qu'il est MANAGER dans la boutique cible
        else
        {
            var membershipOpt = shopMembershipRepository.findActiveMembership(
                    principal.getId(), request.getShopId());

            if (membershipOpt.isEmpty())
            {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Seuls les MANAGERS peuvent crÃ©er des memberships dans cette boutique");
            }

            ShopMembershipEntity membership = membershipOpt.get();
            if (membership.getRole() != RoleEnum.MANAGER)
            {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Seuls les MANAGERS peuvent crÃ©er des memberships (rÃ´le strict, pas override)");
            }
        }

        ShopMembershipEntity membership = identityService.createMembership(
                request.getPersonId(),
                request.getShopId(),
                request.getRole()
        );

        return toMembershipResponse(membership);
    }

    /**
     * Modifie le rÃ´le d'une membership.
     */
    @PreAuthorize("@sec.can(authentication, 'MANAGE_SHOP_SETTINGS')")
    @PutMapping("/memberships/{membershipId}/role")
    @ResponseStatus(HttpStatus.OK)
    public IdentityDTO.MembershipResponse updateMembershipRole(
            @PathVariable UUID membershipId,
            @RequestBody IdentityDTO.UpdateRoleRequest request,
            Authentication authentication)
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);

        ShopMembershipEntity membership = identityService.updateMembershipRole(membershipId, request.getRole());

        // VÃ©rifier que le modificateur a accÃ¨s Ã  la boutique de la membership
        shopAccessGuard.checkShopAccess(principal, membership.getShop().getId());

        return toMembershipResponse(membership);
    }

    /**
     * Active/dÃ©sactive une membership.
     */
    @PreAuthorize("@sec.can(authentication, 'MANAGE_SHOP_SETTINGS')")
    @PatchMapping("/memberships/{membershipId}/toggle-active")
    @ResponseStatus(HttpStatus.OK)
    public IdentityDTO.MembershipResponse toggleMembershipActive(
            @PathVariable UUID membershipId,
            Authentication authentication)
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);

        ShopMembershipEntity membership = identityService.toggleMembershipActive(membershipId);

        // VÃ©rifier que le modificateur a accÃ¨s Ã  la boutique de la membership
        shopAccessGuard.checkShopAccess(principal, membership.getShop().getId());

        return toMembershipResponse(membership);
    }

    /**
     * Supprime une membership.
     */
    @PreAuthorize("@sec.can(authentication, 'MANAGE_SHOP_SETTINGS')")
    @DeleteMapping("/memberships/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMembership(
            @PathVariable UUID membershipId,
            Authentication authentication)
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);

        ShopMembershipEntity membership = shopMembershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership introuvable"));
        shopAccessGuard.checkShopAccess(principal, membership.getShop().getId());
        identityService.deleteMembership(membershipId);
    }

    /**
     * Modifie les permissions overrides d'une membership.
     */
    @PreAuthorize("@sec.can(authentication, 'MANAGE_SHOP_SETTINGS')")
    @PutMapping("/memberships/{membershipId}/permissions")
    @ResponseStatus(HttpStatus.OK)
    public IdentityDTO.MembershipResponse updateMembershipPermissions(
            @PathVariable UUID membershipId,
            @RequestBody IdentityDTO.UpdatePermissionsRequest request,
            Authentication authentication)
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);

        ShopMembershipEntity membership = identityService.updateMembershipPermissions(
                membershipId,
                request.getGrantedOverrides(),
                request.getRevokedOverrides()
        );

        // VÃ©rifier que le modificateur a accÃ¨s Ã  la boutique de la membership
        shopAccessGuard.checkShopAccess(principal, membership.getShop().getId());

        return toMembershipResponse(membership);
    }

    /**
     * Change le numÃ©ro de tÃ©lÃ©phone de la personne authentifiÃ©e.
     */
    @PutMapping("/phone")
    @ResponseStatus(HttpStatus.OK)
    public void changePhoneNumber(
            @RequestBody IdentityDTO.ChangePhoneRequest request,
            Authentication authentication)
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);

        identityService.changePhoneNumber(
                principal.getId(),
                request.getNewPhoneNumber(),
                request.isAdminOverride()
        );
    }

    /**
     * Change l'email de la personne authentifiÃ©e.
     */
    @PutMapping("/email")
    @ResponseStatus(HttpStatus.OK)
    public void changeEmail(
            @RequestBody IdentityDTO.ChangeEmailRequest request,
            Authentication authentication)
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);

        identityService.changeEmail(
                principal.getId(),
                request.getNewEmail(),
                request.isAdminOverride()
        );
    }

    /**
     * RÃ©cupÃ¨re les IDs des boutiques accessibles.
     */
    @GetMapping("/shops")
    @ResponseStatus(HttpStatus.OK)
    public List<UUID> getMyShops(Authentication authentication)
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);
        return identityService.getActiveShopIds(principal.getId());
    }

    /**
     * VÃ©rifie si la personne peut changer son tÃ©lÃ©phone.
     */
    @GetMapping("/can-change-phone")
    @ResponseStatus(HttpStatus.OK)
    public IdentityDTO.CanChangeResponse canChangePhone(Authentication authentication)
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);
        boolean canChange = identityService.canChangePhoneNumber(principal.getId());

        return IdentityDTO.CanChangeResponse.builder()
                .canChange(canChange)
                .reason(canChange ? null : "Limite de 3 mois entre les changements")
                .build();
    }

    /**
     * VÃ©rifie si la personne peut changer son email.
     */
    @GetMapping("/can-change-email")
    @ResponseStatus(HttpStatus.OK)
    public IdentityDTO.CanChangeResponse canChangeEmail(Authentication authentication)
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);
        boolean canChange = identityService.canChangeEmail(principal.getId());

        return IdentityDTO.CanChangeResponse.builder()
                .canChange(canChange)
                .reason(canChange ? null : "Limite de 3 mois entre les changements")
                .build();
    }

    private IdentityDTO.MembershipResponse toMembershipResponse(ShopMembershipEntity membership)
    {
        return IdentityDTO.MembershipResponse.builder()
                .id(membership.getId())
                .personId(membership.getPerson().getId())
                .shopId(membership.getShop().getId())
                .shopName(membership.getShop().getName())
                .role(membership.getRole())
                .active(membership.isActive())
                .joinedAt(membership.getJoinedAt())
                .effectivePermissions(toStringSet(membership.getEffectivePermissions()))
                .grantedOverrides(toStringSet(membership.getGrantedOverrides()))
                .revokedOverrides(toStringSet(membership.getRevokedOverrides()))
                .build();
    }

    private Set<String> toStringSet(Set<PermissionEnum> permissions)
    {
        if (permissions == null)
        {
            return Set.of();
        }
        return permissions.stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
    }
}
