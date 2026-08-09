package com.sellam.store.users.dto;

import com.sellam.store.users.models.RoleEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

public class UserDTO {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CreateUserRequest {
        private String name;
        private String phoneNumber;
        private String password;
        private RoleEnum role;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UpdateUserRequest {
        private String name;
        private String phoneNumber;
        private RoleEnum role;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ChangePasswordRequest {
        private String newPassword;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UserResponse {
        private UUID id;
        private String name;
        private String phoneNumber;
        private String profilePictureUrl;
        private RoleEnum role;
        private boolean active;
        private UUID shopId;
        private String shopName;
    }
}
