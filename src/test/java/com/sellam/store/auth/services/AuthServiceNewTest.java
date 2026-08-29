package com.sellam.store.auth.services;

import com.sellam.store.auth.JwtProviderNew;
import com.sellam.store.auth.dto.AuthDTO;
import com.sellam.store.common.email.services.EmailService;
import com.sellam.store.identity.models.LegacyEntityType;
import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.models.SystemRoleEnum;
import com.sellam.store.identity.repositories.LegacyIdMappingRepository;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.identity.repositories.ShopMembershipRepository;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import com.sellam.store.users.models.RoleEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceNewTest
{

    @Mock
    private PersonRepository personRepository;

    @Mock
    private ShopMembershipRepository shopMembershipRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private LegacyIdMappingRepository legacyIdMappingRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProviderNew jwtProvider;

    private AuthServiceNew authService;

    private UUID personId;
    private UUID shopId;
    private PersonEntity person;
    private ShopEntity shop;

    @BeforeEach
    void setUp()
    {
        authService = new AuthServiceNew(
                personRepository,
                shopMembershipRepository,
                shopRepository,
                legacyIdMappingRepository,
                emailService,
                passwordEncoder,
                jwtProvider,
                true // registration enabled for tests
        );

        personId = UUID.randomUUID();
        shopId = UUID.randomUUID();

        person = PersonEntity.builder()
                .id(personId)
                .name("Test Person")
                .phoneNumber("+33612345678")
                .email("test@example.com")
                .passwordHash("hashed_password")
                .build();

        shop = ShopEntity.builder()
                .id(shopId)
                .name("Test Shop")
                .build();
    }

    @Test
    void register_WithValidData_ShouldSucceed()
    {
        // Given
        AuthDTO.RegisterRequest request = AuthDTO.RegisterRequest.builder()
                .name("New User")
                .phoneNumber("+33698765432")
                .email("new@example.com")
                .password("ValidPass123!")
                .build();

        when(personRepository.findByPhoneNumber("+33698765432")).thenReturn(Optional.empty());
        when(personRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("ValidPass123!")).thenReturn("encoded_password");
        when(personRepository.save(any(PersonEntity.class))).thenReturn(person);
        when(legacyIdMappingRepository.save(any())).thenReturn(null);
        when(jwtProvider.generateToken(personId, "ACCOUNT", null, "+33698765432"))
                .thenReturn("jwt_token");

        // When
        AuthDTO.AuthResponse response = authService.register(request);

        // Then
        assertNotNull(response);
        assertEquals("jwt_token", response.getToken());
        assertEquals("ACCOUNT", response.getUserType());
        assertEquals(personId.toString(), response.getAccountId());
        verify(personRepository).save(any(PersonEntity.class));
        verify(legacyIdMappingRepository).save(any());
    }

    @Test
    void register_WithWeakPassword_ShouldThrowException()
    {
        // Given
        AuthDTO.RegisterRequest request = AuthDTO.RegisterRequest.builder()
                .name("New User")
                .phoneNumber("+33698765432")
                .password("weak") // Trop court, pas de complexité
                .build();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void register_WithDuplicatePhone_ShouldThrowException()
    {
        // Given
        AuthDTO.RegisterRequest request = AuthDTO.RegisterRequest.builder()
                .name("New User")
                .phoneNumber("+33698765432")
                .password("ValidPass123!")
                .build();

        when(personRepository.findByPhoneNumber("+33698765432")).thenReturn(Optional.of(person));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void register_WithoutPhoneOrEmail_ShouldThrowException()
    {
        // Given
        AuthDTO.RegisterRequest request = AuthDTO.RegisterRequest.builder()
                .name("New User")
                .password("ValidPass123!")
                .build(); // Ni phone ni email

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void login_WithPhone_ShouldSucceed()
    {
        // Given
        AuthDTO.LoginRequest request = AuthDTO.LoginRequest.builder()
                .identifier("+33612345678")
                .password("password123")
                .build();

        when(personRepository.findByPhoneNumber("+33612345678")).thenReturn(Optional.of(person));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(shopMembershipRepository.findByPersonId(personId)).thenReturn(List.of()); // Pas de memberships
        when(jwtProvider.generateToken(personId, "ACCOUNT", null, "+33612345678"))
                .thenReturn("jwt_token");

        // When
        AuthDTO.AuthResponse response = authService.login(request);

        // Then
        assertNotNull(response);
        assertEquals("jwt_token", response.getToken());
        assertEquals("ACCOUNT", response.getUserType()); // Pas de memberships = ACCOUNT
        assertNull(response.getShopId());
    }

    @Test
    void login_WithEmail_ShouldSucceed()
    {
        // Given
        AuthDTO.LoginRequest request = AuthDTO.LoginRequest.builder()
                .identifier("test@example.com")
                .password("password123")
                .build();

        when(personRepository.findByEmail("test@example.com")).thenReturn(Optional.of(person));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(shopMembershipRepository.findByPersonId(personId)).thenReturn(List.of());
        when(jwtProvider.generateToken(personId, "ACCOUNT", null, "+33612345678"))
                .thenReturn("jwt_token");

        // When
        AuthDTO.AuthResponse response = authService.login(request);

        // Then
        assertNotNull(response);
        assertEquals("jwt_token", response.getToken());
    }

    @Test
    void login_WithMemberships_ShouldReturnUserType()
    {
        // Given
        AuthDTO.LoginRequest request = AuthDTO.LoginRequest.builder()
                .identifier("+33612345678")
                .password("password123")
                .build();

        // Personne avec une membership
        when(personRepository.findByPhoneNumber("+33612345678")).thenReturn(Optional.of(person));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(shopMembershipRepository.findByPersonId(personId))
                .thenReturn(List.of(createMockMembership(person, shop, RoleEnum.CASHIER)));
        when(jwtProvider.generateToken(personId, "USER", shopId, "+33612345678"))
                .thenReturn("jwt_token");

        // When
        AuthDTO.AuthResponse response = authService.login(request);

        // Then
        assertNotNull(response);
        assertEquals("USER", response.getUserType()); // Avec memberships = USER
        assertEquals(shopId.toString(), response.getShopId());
    }

    @Test
    void login_WithMultipleMemberships_ShouldReturnUserTypeWithFirstActiveShop()
    {
        // Given
        AuthDTO.LoginRequest request = AuthDTO.LoginRequest.builder()
                .identifier("+33612345678")
                .password("password123")
                .build();

        // Personne avec DEUX memberships (cas multi-boutique)
        UUID shopId2 = UUID.randomUUID();
        ShopEntity shop2 = ShopEntity.builder()
                .id(shopId2)
                .name("Second Shop")
                .build();

        when(personRepository.findByPhoneNumber("+33612345678")).thenReturn(Optional.of(person));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(shopMembershipRepository.findByPersonId(personId))
                .thenReturn(List.of(
                        createMockMembership(person, shop, RoleEnum.MANAGER),
                        createMockMembership(person, shop2, RoleEnum.CASHIER)
                ));
        // Doit prendre le premier shop actif comme shopId par défaut (c'est shopId)
        when(jwtProvider.generateToken(eq(personId), eq("USER"), eq(shopId), eq("+33612345678")))
                .thenReturn("jwt_token");

        // When
        AuthDTO.AuthResponse response = authService.login(request);

        // Then
        assertNotNull(response);
        assertEquals("USER", response.getUserType()); // Avec memberships = USER
        assertEquals(shopId.toString(), response.getShopId()); // Premier shop actif (shopId)
    }

    @Test
    void login_WithInvalidCredentials_ShouldThrowException()
    {
        // Given
        AuthDTO.LoginRequest request = AuthDTO.LoginRequest.builder()
                .identifier("+33612345678")
                .password("wrong_password")
                .build();

        when(personRepository.findByPhoneNumber("+33612345678")).thenReturn(Optional.of(person));
        when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void login_WithUnknownIdentifier_ShouldThrowException()
    {
        // Given
        AuthDTO.LoginRequest request = AuthDTO.LoginRequest.builder()
                .identifier("unknown@example.com")
                .password("password123")
                .build();

        when(personRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        when(personRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void verifyEmail_WithValidToken_ShouldSucceed()
    {
        // Given
        String token = "valid_token";
        person.setVerificationToken(token);
        person.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(1));

        when(personRepository.findByVerificationToken(token)).thenReturn(Optional.of(person));

        // When
        authService.verifyEmail(token);

        // Then
        assertTrue(person.isEmailVerified());
        assertNull(person.getVerificationToken());
        verify(personRepository).save(person);
    }

    @Test
    void verifyEmail_WithExpiredToken_ShouldThrowException()
    {
        // Given
        String token = "expired_token";
        person.setVerificationToken(token);
        person.setVerificationTokenExpiresAt(LocalDateTime.now().minusHours(1));

        when(personRepository.findByVerificationToken(token)).thenReturn(Optional.of(person));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> authService.verifyEmail(token));
    }

    private com.sellam.store.identity.models.ShopMembershipEntity createMockMembership(
            PersonEntity person, ShopEntity shop, RoleEnum role)
    {
        return com.sellam.store.identity.models.ShopMembershipEntity.builder()
                .person(person)
                .shop(shop)
                .role(role)
                .active(true)
                .joinedAt(LocalDateTime.now())
                .build();
    }
}
