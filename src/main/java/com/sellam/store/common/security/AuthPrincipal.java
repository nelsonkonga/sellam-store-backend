package com.sellam.store.common.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthPrincipal {
    private UUID id;
    private String name; // Nom de l'utilisateur
    private String userType; // "ACCOUNT" ou "USER"
    private UUID shopId; // nullable, présent uniquement si userType == "USER"
    private String phoneNumber;
}
