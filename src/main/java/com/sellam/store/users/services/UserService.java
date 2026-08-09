package com.sellam.store.users.services;

import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import com.sellam.store.users.dto.UserDTO;
import com.sellam.store.users.models.UserEntity;
import com.sellam.store.users.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, ShopRepository shopRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDTO.UserResponse createUser(UUID shopId, UserDTO.CreateUserRequest request) {
        // Vérifier que le numéro de téléphone n'est pas déjà utilisé
        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
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

    public List<UserDTO.UserResponse> listUsers(UUID shopId) {
        return userRepository.findByShop_Id(shopId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UserDTO.UserResponse getUser(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable"));
        return toResponse(user);
    }

    @Transactional
    public UserDTO.UserResponse updateUser(UUID userId, UserDTO.UpdateUserRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable"));

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getPhoneNumber() != null) {
            // Vérifier l'unicité du nouveau numéro
            userRepository.findByPhoneNumber(request.getPhoneNumber())
                    .filter(existing -> !existing.getId().equals(userId))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Ce numéro de téléphone est déjà utilisé");
                    });
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(UUID userId, UserDTO.ChangePasswordRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable"));
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public UserDTO.UserResponse toggleActive(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable"));
        user.setActive(!user.isActive());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable"));
        userRepository.delete(user);
    }

    private UserDTO.UserResponse toResponse(UserEntity user) {
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
}
