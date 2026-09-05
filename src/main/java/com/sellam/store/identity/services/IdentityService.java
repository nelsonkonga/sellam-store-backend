package com.sellam.store.identity.services;

import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.models.ShopMembershipEntity;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.identity.repositories.ShopMembershipRepository;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import com.sellam.store.users.models.PermissionEnum;
import com.sellam.store.users.models.RoleEnum;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service pour gÃ©rer la logique d'identitÃ© multi-boutique.
 * 
 * FonctionnalitÃ©s :
 * - Gestion des memberships (crÃ©ation, modification, suppression)
 * - Validation des changements de contact (limites de 3 mois)
 * - Override pour rÃ´les PLATFORM_ADMIN
 * - SÃ©lection de boutique active
 */
@Service
@AllArgsConstructor

public class IdentityService
{

    private final PersonRepository personRepository;
    private final ShopMembershipRepository shopMembershipRepository;
    private final ShopRepository shopRepository;

    /**
     * RÃ©cupÃ¨re toutes les memberships actives d'une personne.
     */
    public List<ShopMembershipEntity> getActiveMemberships(UUID personId)
    {
        return shopMembershipRepository.findByPersonId(personId).stream()
                .filter(ShopMembershipEntity::isActive)
                .toList();
    }

    /**
     * RÃ©cupÃ¨re toutes les memberships d'une personne (incluant inactives).
     */
    public List<ShopMembershipEntity> getAllMemberships(UUID personId)
    {
        return shopMembershipRepository.findByPersonId(personId);
    }

    /**
     * RÃ©cupÃ¨re la membership d'une personne pour une boutique spÃ©cifique.
     */
    public ShopMembershipEntity getMembership(UUID personId, UUID shopId)
    {
        return shopMembershipRepository.findActiveMembership(personId, shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership introuvable"));
    }

    /**
     * CrÃ©e une nouvelle membership pour une personne dans une boutique.
     */
    @Transactional
    public ShopMembershipEntity createMembership(UUID personId, UUID shopId, RoleEnum role)
    {
        PersonEntity person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Personne introuvable"));

        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique introuvable"));

        // VÃ©rifier si la membership existe dÃ©jÃ 
        if (shopMembershipRepository.findActiveMembership(personId, shopId).isPresent())
        {
            throw new IllegalArgumentException("Cette personne a dÃ©jÃ  une membership active dans cette boutique");
        }

        ShopMembershipEntity membership = ShopMembershipEntity.builder()
                .person(person)
                .shop(shop)
                .role(role)
                .active(true)
                .joinedAt(LocalDateTime.now())
                .build();

        return shopMembershipRepository.save(membership);
    }

    /**
     * Modifie le rÃ´le d'une membership.
     */
    @Transactional
    public ShopMembershipEntity updateMembershipRole(UUID membershipId, RoleEnum newRole)
    {
        ShopMembershipEntity membership = shopMembershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership introuvable"));

        membership.setRole(newRole);
        return shopMembershipRepository.save(membership);
    }

    /**
     * Active/dÃ©sactive une membership.
     */
    @Transactional
    public ShopMembershipEntity toggleMembershipActive(UUID membershipId)
    {
        ShopMembershipEntity membership = shopMembershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership introuvable"));

        membership.setActive(!membership.isActive());
        return shopMembershipRepository.save(membership);
    }

    /**
     * Supprime une membership.
     */
    @Transactional
    public void deleteMembership(UUID membershipId)
    {
        ShopMembershipEntity membership = shopMembershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership introuvable"));

        shopMembershipRepository.delete(membership);
    }

    /**
     * Modifie les permissions overrides d'une membership.
     */
    @Transactional
    public ShopMembershipEntity updateMembershipPermissions(UUID membershipId,
                                                             Set<PermissionEnum> grantedOverrides,
                                                             Set<PermissionEnum> revokedOverrides)
    {
        ShopMembershipEntity membership = shopMembershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership introuvable"));

        membership.setGrantedOverrides(grantedOverrides);
        membership.setRevokedOverrides(revokedOverrides);
        return shopMembershipRepository.save(membership);
    }

    /**
     * VÃ©rifie si une personne peut changer son numÃ©ro de tÃ©lÃ©phone.
     * Limitation : 1 changement tous les 3 mois, sauf pour PLATFORM_ADMIN.
     */
    public boolean canChangePhoneNumber(UUID personId)
    {
        PersonEntity person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Personne introuvable"));

        // PLATFORM_ADMIN peut toujours changer
        if (person.getSystemRole() != null
                && person.getSystemRole().name().equals("PLATFORM_ADMIN"))
        {
            return true;
        }

        // VÃ©rifier le dÃ©lai de 3 mois
        if (person.getLastPhoneChangeAt() == null)
        {
            return true; // Premier changement autorisÃ©
        }

        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        return person.getLastPhoneChangeAt().isBefore(threeMonthsAgo);
    }

    /**
     * VÃ©rifie si une personne peut changer son email.
     * Limitation : 1 changement tous les 3 mois, sauf pour PLATFORM_ADMIN.
     */
    public boolean canChangeEmail(UUID personId)
    {
        PersonEntity person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Personne introuvable"));

        // PLATFORM_ADMIN peut toujours changer
        if (person.getSystemRole() != null
                && person.getSystemRole().name().equals("PLATFORM_ADMIN"))
        {
            return true;
        }

        // VÃ©rifier le dÃ©lai de 3 mois
        if (person.getLastEmailChangeAt() == null)
        {
            return true; // Premier changement autorisÃ©
        }

        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        return person.getLastEmailChangeAt().isBefore(threeMonthsAgo);
    }

    /**
     * Change le numÃ©ro de tÃ©lÃ©phone d'une personne avec validation.
     */
    @Transactional
    public void changePhoneNumber(UUID personId, String newPhoneNumber, boolean isAdminOverride)
    {
        if (!isAdminOverride && !canChangePhoneNumber(personId))
        {
            throw new IllegalArgumentException("Vous ne pouvez changer votre numÃ©ro de tÃ©lÃ©phone qu'une fois tous les 3 mois");
        }

        PersonEntity person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Personne introuvable"));

        // VÃ©rifier que le nouveau numÃ©ro n'est pas dÃ©jÃ  utilisÃ©
        if (personRepository.findByPhoneNumber(newPhoneNumber).isPresent())
        {
            throw new IllegalArgumentException("Ce numÃ©ro de tÃ©lÃ©phone est dÃ©jÃ  utilisÃ©");
        }

        person.setPhoneNumber(newPhoneNumber);
        person.setLastPhoneChangeAt(LocalDateTime.now());
        personRepository.save(person);
    }

    /**
     * Change l'email d'une personne avec validation.
     */
    @Transactional
    public void changeEmail(UUID personId, String newEmail, boolean isAdminOverride)
    {
        if (!isAdminOverride && !canChangeEmail(personId))
        {
            throw new IllegalArgumentException("Vous ne pouvez changer votre email qu'une fois tous les 3 mois");
        }

        PersonEntity person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Personne introuvable"));

        // VÃ©rifier que le nouvel email n'est pas dÃ©jÃ  utilisÃ©
        if (personRepository.findByEmail(newEmail).isPresent())
        {
            throw new IllegalArgumentException("Cet email est dÃ©jÃ  utilisÃ©");
        }

        person.setEmail(newEmail);
        person.setLastEmailChangeAt(LocalDateTime.now());
        person.setEmailVerified(false); // NÃ©cessite re-vÃ©rification
        personRepository.save(person);
    }

    /**
     * RÃ©cupÃ¨re les IDs des boutiques actives d'une personne.
     */
    public List<UUID> getActiveShopIds(UUID personId)
    {
        return shopMembershipRepository.findActiveShopIdsByPersonId(personId);
    }

    /**
     * VÃ©rifie si une personne a accÃ¨s Ã  une boutique spÃ©cifique.
     */
    public boolean hasAccessToShop(UUID personId, UUID shopId)
    {
        return shopMembershipRepository.findActiveMembership(personId, shopId).isPresent();
    }
}
