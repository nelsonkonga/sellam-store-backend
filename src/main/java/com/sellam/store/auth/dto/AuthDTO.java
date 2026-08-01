package com.sellam.store.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;


public class AuthDTO
{
    @Data
    @AllArgsConstructor
    @Builder
    public static class RegisterInput
    {
        @NonNull
        private String name;

        @NonNull
        private String phoneNumber;

        @NonNull
        private String password;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class LoginInput
    {
        @NonNull
        String phoneNumber;

        @NonNull
        String password;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class AuthOutput
    {
         String token;

         String accountId;

         String name;

         String phoneNumber;

    }
}
