package com.sellam.store.users.services;

import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.identity.models.LegacyEntityType;
import com.sellam.store.identity.models.LegacyIdMapping;
import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.models.ShopMembershipEntity;
import com.sellam.store.identity.repositories.LegacyIdMappingRepository;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.identity.repositories.ShopMembershipRepository;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import com.sellam.store.users.dto.UserDTO;
import com.sellam.store.users.models.PermissionEnum;
import com.sellam.store.users.models.RoleDefaultPermissions;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserService {

    private final PersonRepository personRepository;
    private final ShopMembershipRepository shopMembershipRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;
    private final LegacyIdMappingRepository legacyIdMappingRepository;

    public UUID getShopIdByUserId(UUID userId) {
        List<UUID> shopIds = shopMembershipRepository.findActiveShopIdsByPersonId(userId);
        if (shopIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable ou aucune boutique active");
        }
        return shopIds.get(0);
    }

    @Transactional
    public UserDTO.UserResponse createUser(UUID shopId, UserDTO.CreateUserRequest request) {
        if (personRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new IllegalArgumentException("Ce numéro de téléphone est déjà utilisé par un autre employé");
        }

        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique introuvable"));

        PersonEntity person = PersonEntity.builder()
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        PersonEntity savedPerson = personRepository.save(person);

        // Écrit le mapping legacy USER, comme AuthService.register le fait pour
        // un ACCOUNT. Sans cette ligne, AuthService.login() n'a aucun moyen
        // fiable de reconnaître cette personne comme employé : elle retombait
        // sur une déduction par memberships, fragile et incohérente avec le
        // cas ACCOUNT (voir AuthService.login).
        legacyIdMappingRepository.save(
                LegacyIdMapping.builder()
                        .legacyId(savedPerson.getId())
                        .personId(savedPerson.getId())
                        .legacyType(LegacyEntityType.USER)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        ShopMembershipEntity membership = ShopMembershipEntity.builder()
                .person(savedPerson)
                .shop(shop)
                .role(request.getRole())
                .active(true)
                .build();

        ShopMembershipEntity savedMembership = shopMembershipRepository.save(membership);

        return toResponse(savedPerson, savedMembership);
    }

    public List<UserDTO.UserResponse> listUsers(UUID shopId) {
        return shopMembershipRepository.findByShopId(shopId)
                .stream()
                .map(m -> toResponse(m.getPerson(), m))
                .collect(Collectors.toList());
    }

    public UserDTO.UserResponse getUser(UUID userId) {
        List<ShopMembershipEntity> memberships = shopMembershipRepository.findByPersonId(userId);
        if (memberships.isEmpty()) {
            throw new ResourceNotFoundException("Employé introuvable");
        }
        ShopMembershipEntity m = memberships.get(0);
        return toResponse(m.getPerson(), m);
    }

    /**
     * Retourne l'état actuel des permissions d'un employé : ses permissions
     * par défaut (selon son rôle), ses overrides actuels, et le résultat
     * effectif combiné.
     */
    public UserDTO.PermissionsResponse getPermissions(UUID userId) {
        List<ShopMembershipEntity> memberships = shopMembershipRepository.findByPersonId(userId);
        if (memberships.isEmpty()) {
            throw new ResourceNotFoundException("Employé introuvable");
        }
        ShopMembershipEntity m = memberships.get(0);

        Set<PermissionEnum> defaults = RoleDefaultPermissions.forRole(m.getRole());
        Set<PermissionEnum> effective = m.getEffectivePermissions();

        return UserDTO.PermissionsResponse.builder()
                .role(m.getRole() != null ? m.getRole().name() : null)
                .defaultPermissions(toStringSet(defaults))
                .grantedOverrides(toStringSet(m.getGrantedOverrides()))
                .revokedOverrides(toStringSet(m.getRevokedOverrides()))
                .effectivePermissions(toStringSet(effective))
                .build();
    }

    /**
     * Remplace entièrement les overrides de permissions d'un employé
     * (granted et revoked). Le gérant envoie l'état complet souhaité,
     * pas un delta.
     */
    @Transactional
    public UserDTO.PermissionsResponse updatePermissions(UUID userId, UserDTO.UpdatePermissionsRequest request) {
        List<ShopMembershipEntity> memberships = shopMembershipRepository.findByPersonId(userId);
        if (memberships.isEmpty()) {
            throw new ResourceNotFoundException("Employé introuvable");
        }
        ShopMembershipEntity m = memberships.get(0);

        m.setGrantedOverrides(toPermissionSet(request.getGrantedOverrides()));
        m.setRevokedOverrides(toPermissionSet(request.getRevokedOverrides()));

        shopMembershipRepository.save(m);

        return getPermissions(userId);
    }

    @Transactional
    public UserDTO.UserResponse updateUser(UUID userId, UserDTO.UpdateUserRequest request) {
        List<ShopMembershipEntity> memberships = shopMembershipRepository.findByPersonId(userId);
        if (memberships.isEmpty()) {
            throw new ResourceNotFoundException("Employé introuvable");
        }
        ShopMembershipEntity m = memberships.get(0);
        PersonEntity person = m.getPerson();

        if (request.getName() != null) {
            person.setName(request.getName());
        }
        if (request.getPhoneNumber() != null) {
            personRepository.findByPhoneNumber(request.getPhoneNumber())
                    .filter(existing -> !existing.getId().equals(userId))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Ce numéro de téléphone est déjà utilisé");
                    });
            person.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getRole() != null) {
            m.setRole(request.getRole());
        }

        personRepository.save(person);
        shopMembershipRepository.save(m);

        return toResponse(person, m);
    }

    @Transactional
    public void changePassword(UUID userId, UserDTO.ChangePasswordRequest request) {
        PersonEntity person = personRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable"));
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8)
        {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule, un chiffre et un caractère spécial.");
        }
        person.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        person.setTokenVersion(person.getTokenVersion() + 1);
        personRepository.save(person);
    }

    @Transactional
    public UserDTO.UserResponse toggleActive(UUID userId) {
        List<ShopMembershipEntity> memberships = shopMembershipRepository.findByPersonId(userId);
        if (memberships.isEmpty()) {
            throw new ResourceNotFoundException("Employé introuvable");
        }
        ShopMembershipEntity m = memberships.get(0);
        m.setActive(!m.isActive());
        shopMembershipRepository.save(m);
        return toResponse(m.getPerson(), m);
    }

    @Transactional
    public void deleteUser(UUID userId, UUID shopId) {
        List<ShopMembershipEntity> memberships = shopMembershipRepository.findByPersonId(userId);
        List<ShopMembershipEntity> inThisShop = memberships.stream()
                .filter(membership -> membership.getShop() != null && shopId.equals(membership.getShop().getId()))
                .toList();
        if (!inThisShop.isEmpty()) {
            shopMembershipRepository.deleteAll(inThisShop);
        }
        if (memberships.size() == inThisShop.size()) {
            personRepository.findById(userId).ifPresent(personRepository::delete);
        }
    }

    private Set<String> toStringSet(Set<PermissionEnum> permissions) {
        if (permissions == null) {
            return Set.of();
        }
        return permissions.stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
    }

    private Set<PermissionEnum> toPermissionSet(Set<String> names) {
        if (names == null) {
            return new HashSet<>();
        }
        Set<PermissionEnum> result = new HashSet<>();
        for (String name : names) {
            try {
                result.add(PermissionEnum.valueOf(name));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Permission inconnue : " + name);
            }
        }
        return result;
    }

    private UserDTO.UserResponse toResponse(PersonEntity person, ShopMembershipEntity membership) {
        return UserDTO.UserResponse.builder()
                .id(person.getId())
                .name(person.getName())
                .phoneNumber(person.getPhoneNumber())
                .profilePictureUrl(person.getProfilePictureUrl())
                .role(membership.getRole())
                .active(membership.isActive())
                .shopId(membership.getShop().getId())
                .shopName(membership.getShop().getName())
                .build();
    }

    public void updateProfilePictureUrl(UUID userId, String profilePictureUrl) {
        PersonEntity person = personRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur Introuvable"));
        person.setProfilePictureUrl(profilePictureUrl);
        personRepository.save(person);
    }
}