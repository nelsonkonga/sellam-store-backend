package com.sellam.store.accounts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
@Builder
public class RegisterRequestsDTO
{
    public static class PostInput
    {
        @NonNull
        String name;

        @NonNull
        String phoneNumber;

        @NonNull
        String password;
    }
}
