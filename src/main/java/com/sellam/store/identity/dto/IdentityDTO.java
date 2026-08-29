package com.sellam.store.identity.dto;

import com.sellam.store.users.models.PermissionEnum;
import com.sellam.store.users.models.RoleEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * DTOs pour les opérations d'identité et de memberships.
 */
public class IdentityDTO
{
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class MembershipResponse
    {
        private UUID id;
        private UUID personId;
        private UUID shopId;
        private String shopName;
        private RoleEnum role;
        private boolean active;
        private LocalDateTime joinedAt;
        private Set<String> effectivePermissions;
        private Set<String> grantedOverrides;
        private Set<String> revokedOverrides;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CreateMembershipRequest
    {
        private UUID personId;
        private UUID shopId;
        private RoleEnum role;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UpdateRoleRequest
    {
        private RoleEnum role;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UpdatePermissionsRequest
    {
        private Set<PermissionEnum> grantedOverrides;
        private Set<PermissionEnum> revokedOverrides;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ChangePhoneRequest
    {
        private String newPhoneNumber;
        private boolean adminOverride; // Pour override par PLATFORM_ADMIN
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ChangeEmailRequest
    {
        private String newEmail;
        private boolean adminOverride; // Pour override par PLATFORM_ADMIN
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CanChangeResponse
    {
        private boolean canChange;
        private String reason;
    }
}
