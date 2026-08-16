package com.sellam.store.users.services;

import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import com.sellam.store.users.dto.UserDTO;
import com.sellam.store.users.models.PermissionEnum;
import com.sellam.store.users.models.RoleDefaultPermissions;
import com.sellam.store.users.models.UserEntity;
import com.sellam.store.users.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserService
{

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;


    public UUID getShopIdByUserId(UUID userId)
    {
        return userRepository.findShopIdByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    @Transactional
    public UserDTO.UserResponse createUser(UUID shopId, UserDTO.CreateUserRequest request) {
        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent())
        {
            throw new IllegalArgumentException("Ce numéro de téléphone est déjà utilisé par un autre employé");
        }

        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique introuvable"));

        UserEntity user = UserEntity.builder()
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .active(true)
                .shop(shop)
                .build();

        UserEntity saved = userRepository.save(user);
        return toResponse(saved);
    }

    public List<UserDTO.UserResponse> listUsers(UUID shopId)
    {
        return userRepository.findByShop_Id(shopId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UserDTO.UserResponse getUser(UUID userId)
    {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable"));
        return toResponse(user);
    }

    /**
     * Retourne l'état actuel des permissions d'un employé : ses permissions
     * par défaut (selon son rôle), ses overrides actuels, et le résultat
     * effectif combiné.
     */
    public UserDTO.PermissionsResponse getPermissions(UUID userId)
    {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable"));

        Set<PermissionEnum> defaults = RoleDefaultPermissions.forRole(user.getRole());
        Set<PermissionEnum> effective = user.getEffectivePermissions();

        return UserDTO.PermissionsResponse.builder()
                .role(user.getRole() != null ? user.getRole().name() : null)
                .defaultPermissions(toStringSet(defaults))
                .grantedOverrides(toStringSet(user.getGrantedOverrides()))
                .revokedOverrides(toStringSet(user.getRevokedOverrides()))
                .effectivePermissions(toStringSet(effective))
                .build();
    }

    /**
     * Remplace entièrement les overrides de permissions d'un employé
     * (granted et revoked). Le gérant envoie l'état complet souhaité,
     * pas un delta.
     */
    @Transactional
    public UserDTO.PermissionsResponse updatePermissions(UUID userId, UserDTO.UpdatePermissionsRequest request)
    {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable"));

        user.setGrantedOverrides(toPermissionSet(request.getGrantedOverrides()));
        user.setRevokedOverrides(toPermissionSet(request.getRevokedOverrides()));

        userRepository.save(user);

        return getPermissions(userId);
    }

    @Transactional
    public UserDTO.UserResponse updateUser(UUID userId, UserDTO.UpdateUserRequest request)
    {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable"));

        if (request.getName() != null)
        {
            user.setName(request.getName());
        }
        if (request.getPhoneNumber() != null)
        {
            userRepository.findByPhoneNumber(request.getPhoneNumber())
                    .filter(existing -> !existing.getId().equals(userId))
                    .ifPresent(existing ->
                    {
                        throw new IllegalArgumentException("Ce numéro de téléphone est déjà utilisé");
                    });
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getRole() != null)
        {
            user.setRole(request.getRole());
        }

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(UUID userId, UserDTO.ChangePasswordRequest request)
    {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable"));
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public UserDTO.UserResponse toggleActive(UUID userId)
    {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable"));
        user.setActive(!user.isActive());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(UUID userId)
    {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable"));
        userRepository.delete(user);
    }

    private Set<String> toStringSet(Set<PermissionEnum> permissions)
    {
        if (permissions == null)
        {
            return Set.of();
        }
        return permissions.stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
    }

    private Set<PermissionEnum> toPermissionSet(Set<String> names)
    {
        if (names == null)
        {
            return new HashSet<>();
        }
        Set<PermissionEnum> result = new HashSet<>();
        for (String name : names)
        {
            try
            {
                result.add(PermissionEnum.valueOf(name));
            }
            catch (IllegalArgumentException e)
            {
                throw new IllegalArgumentException("Permission inconnue : " + name);
            }
        }
        return result;
    }

    private UserDTO.UserResponse toResponse(UserEntity user)
    {
        return UserDTO.UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .profilePictureUrl(user.getProfilePictureUrl())
                .role(user.getRole())
                .active(user.isActive())
                .shopId(user.getShop().getId())
                .shopName(user.getShop().getName())
                .build();
    }

    public void updateProfilePictureUrl(UUID userId, String profilePictureUrl)
    {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur Introuvable"));
        user.setProfilePictureUrl(profilePictureUrl);
        userRepository.save(user);
    }
}
