package com.sellam.store.users.services;

import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import com.sellam.store.users.dto.UserDTO;
import com.sellam.store.users.models.PermissionEnum;
import com.sellam.store.users.models.RoleEnum;
import com.sellam.store.users.models.UserEntity;
import com.sellam.store.users.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private ShopRepository shopRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    private UUID shopId;
    private UUID userId;
    private ShopEntity shop;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        shopRepository = mock(ShopRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userService = new UserService(userRepository, shopRepository, passwordEncoder);

        shopId = UUID.randomUUID();
        userId = UUID.randomUUID();

        shop = ShopEntity.builder()
                .id(shopId)
                .name("Test Shop")
                .build();
    }

    @Test
    void getShopIdByUserId_shouldReturnShopId() {
        when(userRepository.findShopIdByUserId(userId)).thenReturn(Optional.of(shopId));

        UUID result = userService.getShopIdByUserId(userId);

        assertEquals(shopId, result);
    }

    @Test
    void getShopIdByUserId_shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findShopIdByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> userService.getShopIdByUserId(userId));
    }

    @Test
    void createUser_shouldCreateUserSuccessfully() {
        when(userRepository.findByPhoneNumber("690000000")).thenReturn(Optional.empty());
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO.CreateUserRequest request = UserDTO.CreateUserRequest.builder()
                .name("Jean Dupont")
                .phoneNumber("690000000")
                .password("secret123")
                .role(RoleEnum.CASHIER)
                .build();

        UserDTO.UserResponse response = userService.createUser(shopId, request);

        assertNotNull(response);
        assertEquals("Jean Dupont", response.getName());
        assertEquals("690000000", response.getPhoneNumber());
        assertEquals(RoleEnum.CASHIER, response.getRole());
        assertTrue(response.isActive());
        assertEquals(shopId, response.getShopId());

        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void createUser_shouldThrowExceptionWhenPhoneNumberAlreadyExists() {
        UserEntity existingUser = UserEntity.builder()
                .id(UUID.randomUUID())
                .phoneNumber("690000000")
                .build();

        when(userRepository.findByPhoneNumber("690000000")).thenReturn(Optional.of(existingUser));

        UserDTO.CreateUserRequest request = UserDTO.CreateUserRequest.builder()
                .name("Jean Dupont")
                .phoneNumber("690000000")
                .password("secret123")
                .role(RoleEnum.CASHIER)
                .build();

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(shopId, request));
    }

    @Test
    void createUser_shouldThrowExceptionWhenShopNotFound() {
        when(userRepository.findByPhoneNumber("690000000")).thenReturn(Optional.empty());
        when(shopRepository.findById(shopId)).thenReturn(Optional.empty());

        UserDTO.CreateUserRequest request = UserDTO.CreateUserRequest.builder()
                .name("Jean Dupont")
                .phoneNumber("690000000")
                .password("secret123")
                .role(RoleEnum.CASHIER)
                .build();

        assertThrows(ResourceNotFoundException.class, () -> userService.createUser(shopId, request));
    }

    @Test
    void listUsers_shouldReturnUsersForShop() {
        UserEntity user1 = UserEntity.builder()
                .id(UUID.randomUUID())
                .name("Jean Dupont")
                .phoneNumber("690000000")
                .role(RoleEnum.CASHIER)
                .active(true)
                .shop(shop)
                .build();

        UserEntity user2 = UserEntity.builder()
                .id(UUID.randomUUID())
                .name("Marie Curie")
                .phoneNumber("690000001")
                .role(RoleEnum.MANAGER)
                .active(true)
                .shop(shop)
                .build();

        when(userRepository.findByShop_Id(shopId)).thenReturn(Arrays.asList(user1, user2));

        List<UserDTO.UserResponse> users = userService.listUsers(shopId);

        assertEquals(2, users.size());
        assertEquals("Jean Dupont", users.get(0).getName());
        assertEquals("Marie Curie", users.get(1).getName());
    }

    @Test
    void listUsers_shouldReturnEmptyListWhenNoUsers() {
        when(userRepository.findByShop_Id(shopId)).thenReturn(Arrays.asList());

        List<UserDTO.UserResponse> users = userService.listUsers(shopId);

        assertTrue(users.isEmpty());
    }

    @Test
    void getUser_shouldReturnUser() {
        UserEntity user = UserEntity.builder()
                .id(userId)
                .name("Jean Dupont")
                .phoneNumber("690000000")
                .role(RoleEnum.CASHIER)
                .active(true)
                .shop(shop)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserDTO.UserResponse response = userService.getUser(userId);

        assertNotNull(response);
        assertEquals("Jean Dupont", response.getName());
        assertEquals("690000000", response.getPhoneNumber());
        assertEquals(RoleEnum.CASHIER, response.getRole());
    }

    @Test
    void getUser_shouldThrowExceptionWhenNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUser(userId));
    }

    @Test
    void getPermissions_shouldReturnPermissions() {
        Set<PermissionEnum> defaultPermissions = Set.of(PermissionEnum.VIEW_SALES_HISTORY, PermissionEnum.CREATE_INVOICE);
        Set<PermissionEnum> effectivePermissions = Set.of(PermissionEnum.VIEW_SALES_HISTORY, PermissionEnum.CREATE_INVOICE, PermissionEnum.VIEW_PRODUCTS);

        UserEntity user = UserEntity.builder()
                .id(userId)
                .name("Jean Dupont")
                .role(RoleEnum.CASHIER)
                .active(true)
                .shop(shop)
                .grantedOverrides(Set.of(PermissionEnum.VIEW_PRODUCTS))
                .revokedOverrides(Set.of())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserDTO.PermissionsResponse response = userService.getPermissions(userId);

        assertNotNull(response);
        assertEquals(RoleEnum.CASHIER.name(), response.getRole());
        assertNotNull(response.getDefaultPermissions());
        assertNotNull(response.getEffectivePermissions());
    }

    @Test
    void getPermissions_shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getPermissions(userId));
    }

    @Test
    void updatePermissions_shouldUpdatePermissions() {
        UserEntity user = UserEntity.builder()
                .id(userId)
                .name("Jean Dupont")
                .role(RoleEnum.CASHIER)
                .active(true)
                .shop(shop)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO.UpdatePermissionsRequest request = UserDTO.UpdatePermissionsRequest.builder()
                .grantedOverrides(Set.of("VIEW_PRODUCTS", "MODIFY_PRODUCTS"))
                .revokedOverrides(Set.of("DELETE_PRODUCTS"))
                .build();

        UserDTO.PermissionsResponse response = userService.updatePermissions(userId, request);

        assertNotNull(response);
        verify(userRepository).save(user);
    }

    @Test
    void updatePermissions_shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UserDTO.UpdatePermissionsRequest request = UserDTO.UpdatePermissionsRequest.builder()
                .grantedOverrides(Set.of("VIEW_PRODUCTS"))
                .build();

        assertThrows(ResourceNotFoundException.class, () -> userService.updatePermissions(userId, request));
    }

    @Test
    void updateUser_shouldUpdateUserName() {
        UserEntity user = UserEntity.builder()
                .id(userId)
                .name("Jean Dupont")
                .phoneNumber("690000000")
                .role(RoleEnum.CASHIER)
                .active(true)
                .shop(shop)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO.UpdateUserRequest request = UserDTO.UpdateUserRequest.builder()
                .name("Jean Pierre")
                .build();

        UserDTO.UserResponse response = userService.updateUser(userId, request);

        assertNotNull(response);
        assertEquals("Jean Pierre", response.getName());
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_shouldThrowExceptionWhenPhoneNumberAlreadyExists() {
        UserEntity user = UserEntity.builder()
                .id(userId)
                .name("Jean Dupont")
                .phoneNumber("690000000")
                .role(RoleEnum.CASHIER)
                .active(true)
                .shop(shop)
                .build();

        UserEntity otherUser = UserEntity.builder()
                .id(UUID.randomUUID())
                .phoneNumber("690000001")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByPhoneNumber("690000001")).thenReturn(Optional.of(otherUser));

        UserDTO.UpdateUserRequest request = UserDTO.UpdateUserRequest.builder()
                .phoneNumber("690000001")
                .build();

        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(userId, request));
    }

    @Test
    void changePassword_shouldChangePassword() {
        UserEntity user = UserEntity.builder()
                .id(userId)
                .name("Jean Dupont")
                .passwordHash("old-encoded-password")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-password")).thenReturn("new-encoded-password");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO.ChangePasswordRequest request = UserDTO.ChangePasswordRequest.builder()
                .newPassword("new-password")
                .build();

        userService.changePassword(userId, request);

        assertEquals("new-encoded-password", user.getPasswordHash());
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UserDTO.ChangePasswordRequest request = UserDTO.ChangePasswordRequest.builder()
                .newPassword("new-password")
                .build();

        assertThrows(ResourceNotFoundException.class, () -> userService.changePassword(userId, request));
    }

    @Test
    void toggleActive_shouldToggleActiveStatus() {
        UserEntity user = UserEntity.builder()
                .id(userId)
                .name("Jean Dupont")
                .active(true)
                .shop(shop)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO.UserResponse response = userService.toggleActive(userId);

        assertNotNull(response);
        assertFalse(response.isActive());
        verify(userRepository).save(user);
    }

    @Test
    void toggleActive_shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.toggleActive(userId));
    }

    @Test
    void deleteUser_shouldDeleteUser() {
        UserEntity user = UserEntity.builder()
                .id(userId)
                .name("Jean Dupont")
                .shop(shop)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).delete(user);

        userService.deleteUser(userId);

        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(userId));
    }

    @Test
    void updateProfilePictureUrl_shouldUpdateProfilePicture() {
        UserEntity user = UserEntity.builder()
                .id(userId)
                .name("Jean Dupont")
                .profilePictureUrl(null)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String newPictureUrl = "https://example.com/profile.jpg";
        userService.updateProfilePictureUrl(userId, newPictureUrl);

        assertEquals(newPictureUrl, user.getProfilePictureUrl());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfilePictureUrl_shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateProfilePictureUrl(userId, "https://example.com/profile.jpg"));
    }
}