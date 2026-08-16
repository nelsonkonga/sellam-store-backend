package com.sellam.store.auth.services;

import com.sellam.store.accounts.models.AccountEntity;
import com.sellam.store.accounts.repositories.AccountRepository;
import com.sellam.store.auth.JwtProvider;
import com.sellam.store.auth.dto.AuthDTO;
import com.sellam.store.common.email.services.EmailService;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.users.models.RoleEnum;
import com.sellam.store.users.models.UserEntity;
import com.sellam.store.users.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AuthServiceLoginTest {

    @Test
    void login_shouldAuthenticateByEmailIdentifier() {
        AccountRepository accountRepository = Mockito.mock(AccountRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        EmailService emailService = Mockito.mock(EmailService.class);
        JwtProvider jwtProvider = Mockito.mock(JwtProvider.class);

        AccountEntity account = AccountEntity.builder()
                .id(UUID.randomUUID())
                .name("Alice")
                .email("alice@example.com")
                .passwordHash(new BCryptPasswordEncoder().encode("secret123"))
                .build();

        when(accountRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(account));
        when(accountRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());
        when(jwtProvider.generateToken(account.getId(), "ACCOUNT", null, account.getPhoneNumber()))
                .thenReturn("jwt-token");

        AuthService authService = new AuthService(
                accountRepository,
                userRepository,
                emailService,
                new BCryptPasswordEncoder(),
                jwtProvider
        );

        AuthDTO.AuthResponse response = authService.login(
                AuthDTO.LoginRequest.builder()
                        .identifier("alice@example.com")
                        .password("secret123")
                        .build()
        );

        assertEquals("ACCOUNT", response.getUserType());
        assertEquals("alice@example.com", response.getEmail());
        assertEquals("jwt-token", response.getToken());
    }

    @Test
    void login_shouldAuthenticatePhoneNumberWithOrWithoutCountryCode() {
        AccountRepository accountRepository = Mockito.mock(AccountRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        EmailService emailService = Mockito.mock(EmailService.class);
        JwtProvider jwtProvider = Mockito.mock(JwtProvider.class);

        AccountEntity account = AccountEntity.builder()
                .id(UUID.randomUUID())
                .name("Alice")
                .phoneNumber("690000000")
                .passwordHash(new BCryptPasswordEncoder().encode("secret123"))
                .build();

        when(accountRepository.findByPhoneNumber("+237690000000")).thenReturn(Optional.empty());
        when(accountRepository.findByPhoneNumber("237690000000")).thenReturn(Optional.empty());
        when(accountRepository.findByPhoneNumber("690000000")).thenReturn(Optional.of(account));
        when(jwtProvider.generateToken(account.getId(), "ACCOUNT", null, account.getPhoneNumber()))
                .thenReturn("jwt-token-phone");

        AuthService authService = new AuthService(
                accountRepository,
                userRepository,
                emailService,
                new BCryptPasswordEncoder(),
                jwtProvider
        );

        AuthDTO.AuthResponse response = authService.login(
                AuthDTO.LoginRequest.builder()
                        .identifier("+237690000000")
                        .password("secret123")
                        .build()
        );

        assertEquals("ACCOUNT", response.getUserType());
        assertEquals("690000000", response.getPhoneNumber());
        assertEquals("jwt-token-phone", response.getToken());
    }

    @Test
    void login_shouldAuthenticateUserByEmailIdentifier() {
        AccountRepository accountRepository = Mockito.mock(AccountRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        EmailService emailService = Mockito.mock(EmailService.class);
        JwtProvider jwtProvider = Mockito.mock(JwtProvider.class);

        ShopEntity shop = ShopEntity.builder()
                .id(UUID.randomUUID())
                .name("Shop A")
                .build();

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .name("Bob")
                .email("bob@example.com")
                .phoneNumber("690000001")
                .passwordHash(new BCryptPasswordEncoder().encode("secret123"))
                .active(true)
                .role(RoleEnum.CASHIER)
                .shop(shop)
                .build();

        when(accountRepository.findByEmail("bob@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));
        when(jwtProvider.generateToken(user.getId(), "USER", shop.getId(), user.getPhoneNumber()))
                .thenReturn("jwt-token-user");

        AuthService authService = new AuthService(
                accountRepository,
                userRepository,
                emailService,
                new BCryptPasswordEncoder(),
                jwtProvider
        );

        AuthDTO.AuthResponse response = authService.login(
                AuthDTO.LoginRequest.builder()
                        .identifier("bob@example.com")
                        .password("secret123")
                        .build()
        );

        assertEquals("USER", response.getUserType());
        assertEquals("bob@example.com", response.getEmail());
        assertEquals("jwt-token-user", response.getToken());
    }
}
