package com.sellam.store.users.dto;

import com.sellam.store.users.models.RoleEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Pattern;

import java.util.Set;
import java.util.UUID;

public class UserDTO
{

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CreateUserRequest
    {
        private String name;
        @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "Le numéro doit être au format international E.164 (ex: +237690000000)")
        private String phoneNumber;
        private String password;
        private RoleEnum role;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UpdateUserRequest
    {
        private String name;
        @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "Le numéro doit être au format international E.164 (ex: +237690000000)")
        private String phoneNumber;
        private RoleEnum role;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ChangePasswordRequest
    {
        private String newPassword;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UserResponse
    {
        private UUID id;
        private String name;
        private String phoneNumber;
        private String profilePictureUrl;
        private RoleEnum role;
        private boolean active;
        private UUID shopId;
        private String shopName;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UpdatePermissionsRequest
    {
        private Set<String> grantedOverrides;
        private Set<String> revokedOverrides;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class PermissionsResponse
    {
        private String role;
        private Set<String> defaultPermissions;
        private Set<String> grantedOverrides;
        private Set<String> revokedOverrides;
        private Set<String> effectivePermissions;
    }
}
