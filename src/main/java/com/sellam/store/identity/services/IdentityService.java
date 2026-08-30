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
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service pour gérer la logique d'identité multi-boutique.
 * 
 * Fonctionnalités :
 * - Gestion des memberships (création, modification, suppression)
 * - Validation des changements de contact (limites de 3 mois)
 * - Override pour rôles PLATFORM_ADMIN
 * - Sélection de boutique active
 */
@Service
@AllArgsConstructor
@Profile("phase1")
public class IdentityService
{

    private final PersonRepository personRepository;
    private final ShopMembershipRepository shopMembershipRepository;
    private final ShopRepository shopRepository;

    /**
     * Récupère toutes les memberships actives d'une personne.
     */
    public List<ShopMembershipEntity> getActiveMemberships(UUID personId)
    {
        return shopMembershipRepository.findByPersonId(personId).stream()
                .filter(ShopMembershipEntity::isActive)
                .toList();
    }

    /**
     * Récupère toutes les memberships d'une personne (incluant inactives).
     */
    public List<ShopMembershipEntity> getAllMemberships(UUID personId)
    {
        return shopMembershipRepository.findByPersonId(personId);
    }

    /**
     * Récupère la membership d'une personne pour une boutique spécifique.
     */
    public ShopMembershipEntity getMembership(UUID personId, UUID shopId)
    {
        return shopMembershipRepository.findActiveMembership(personId, shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership introuvable"));
    }

    /**
     * Crée une nouvelle membership pour une personne dans une boutique.
     */
    @Transactional
    public ShopMembershipEntity createMembership(UUID personId, UUID shopId, RoleEnum role)
    {
        PersonEntity person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Personne introuvable"));

        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique introuvable"));

        // Vérifier si la membership existe déjà
        if (shopMembershipRepository.findActiveMembership(personId, shopId).isPresent())
        {
            throw new IllegalArgumentException("Cette personne a déjà une membership active dans cette boutique");
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
     * Modifie le rôle d'une membership.
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
     * Active/désactive une membership.
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
     * Vérifie si une personne peut changer son numéro de téléphone.
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

        // Vérifier le délai de 3 mois
        if (person.getLastPhoneChangeAt() == null)
        {
            return true; // Premier changement autorisé
        }

        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        return person.getLastPhoneChangeAt().isBefore(threeMonthsAgo);
    }

    /**
     * Vérifie si une personne peut changer son email.
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

        // Vérifier le délai de 3 mois
        if (person.getLastEmailChangeAt() == null)
        {
            return true; // Premier changement autorisé
        }

        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        return person.getLastEmailChangeAt().isBefore(threeMonthsAgo);
    }

    /**
     * Change le numéro de téléphone d'une personne avec validation.
     */
    @Transactional
    public void changePhoneNumber(UUID personId, String newPhoneNumber, boolean isAdminOverride)
    {
        if (!isAdminOverride && !canChangePhoneNumber(personId))
        {
            throw new IllegalArgumentException("Vous ne pouvez changer votre numéro de téléphone qu'une fois tous les 3 mois");
        }

        PersonEntity person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Personne introuvable"));

        // Vérifier que le nouveau numéro n'est pas déjà utilisé
        if (personRepository.findByPhoneNumber(newPhoneNumber).isPresent())
        {
            throw new IllegalArgumentException("Ce numéro de téléphone est déjà utilisé");
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

        // Vérifier que le nouvel email n'est pas déjà utilisé
        if (personRepository.findByEmail(newEmail).isPresent())
        {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        person.setEmail(newEmail);
        person.setLastEmailChangeAt(LocalDateTime.now());
        person.setEmailVerified(false); // Nécessite re-vérification
        personRepository.save(person);
    }

    /**
     * Récupère les IDs des boutiques actives d'une personne.
     */
    public List<UUID> getActiveShopIds(UUID personId)
    {
        return shopMembershipRepository.findActiveShopIdsByPersonId(personId);
    }

    /**
     * Vérifie si une personne a accès à une boutique spécifique.
     */
    public boolean hasAccessToShop(UUID personId, UUID shopId)
    {
        return shopMembershipRepository.findActiveMembership(personId, shopId).isPresent();
    }
}
