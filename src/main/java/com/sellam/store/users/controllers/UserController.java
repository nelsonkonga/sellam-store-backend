package com.sellam.store.users.controllers;

import com.sellam.store.common.security.AuthPrincipal;
import com.sellam.store.users.dto.UserDTO;
import com.sellam.store.users.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/shop/{shopId}")
    public ResponseEntity<UserDTO.UserResponse> createUser(
            @PathVariable UUID shopId,
            @RequestBody UserDTO.CreateUserRequest request,
            Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        assert principal != null;
        if ("USER".equals(principal.getUserType())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(shopId, request));
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<List<UserDTO.UserResponse>> listUsers(
            @PathVariable UUID shopId,
            Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        // Les employés ne peuvent voir que les employés de leur propre boutique
        if ("USER".equals(principal.getUserType()) && !shopId.equals(principal.getShopId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(userService.listUsers(shopId));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDTO.UserResponse> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserDTO.UserResponse> updateUser(
            @PathVariable UUID userId,
            @RequestBody UserDTO.UpdateUserRequest request,
            Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        if ("USER".equals(principal.getUserType())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    @PatchMapping("/{userId}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable UUID userId,
            @RequestBody UserDTO.ChangePasswordRequest request,
            Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        if ("USER".equals(principal.getUserType())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        userService.changePassword(userId, request);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{userId}/toggle-active")
    public ResponseEntity<UserDTO.UserResponse> toggleActive(
            @PathVariable UUID userId,
            Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        if ("USER".equals(principal.getUserType())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(userService.toggleActive(userId));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID userId,
            Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        if ("USER".equals(principal.getUserType())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
