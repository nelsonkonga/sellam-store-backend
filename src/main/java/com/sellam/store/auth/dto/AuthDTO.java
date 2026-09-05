package com.sellam.store.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;


public class AuthDTO
{
    @Data
    @AllArgsConstructor
    @Builder
    public static class RegisterRequest
    {
        @NotBlank(message = "Le nom est requis")
        private String name;

        @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "Le numéro doit être au format international E.164 (ex: +237690000000)")
        private String phoneNumber;

        @NotBlank(message = "Le mot de passe est requis")
        private String password;

        @jakarta.validation.constraints.Email(message = "Format d'email invalide")
        private String email;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class LoginRequest
    {
        @NotBlank(message = "L'identifiant (email ou téléphone) est requis")
        private String identifier;

        @NotBlank(message = "Le mot de passe est requis")
        private String password;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class AuthResponse
    {
         String token;

         String accountId;

         String userType;

         String shopId;

         String name;

         String phoneNumber;

        String email;

        boolean emailVerified;

    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class ForgotPasswordRequest
    {
        @NonNull
        @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "Le numéro doit être au format international E.164 (ex: +237690000000)")
        private String phoneNumber;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class ForgotPasswordResponse
    {
        private String message;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class ResetPasswordRequest
    {
        @NonNull
        private String resetToken;

        @NonNull
        private String newPassword;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class ResetPasswordResponse
    {
        private String message;
    }
}
