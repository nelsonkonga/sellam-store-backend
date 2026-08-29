package com.sellam.store.auth.services;

import com.sellam.store.auth.JwtProviderNew;
import com.sellam.store.auth.dto.AuthDTO;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.identity.repositories.ShopMembershipRepository;
import com.sellam.store.shops.repositories.ShopRepository;
import com.sellam.store.identity.repositories.LegacyIdMappingRepository;
import com.sellam.store.common.email.services.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test pour valider le blocage technique des inscriptions pendant la fenêtre canary.
 */
@ExtendWith(MockitoExtension.class)
class RegistrationBlockTest
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

    @Test
    void register_WhenRegistrationDisabled_ShouldThrowException()
    {
        // Given - Service avec registration désactivée
        AuthServiceNew authService = new AuthServiceNew(
                personRepository,
                shopMembershipRepository,
                shopRepository,
                legacyIdMappingRepository,
                emailService,
                passwordEncoder,
                jwtProvider,
                false // registration disabled
        );

        AuthDTO.RegisterRequest request = AuthDTO.RegisterRequest.builder()
                .name("Test User")
                .phoneNumber("+33612345678")
                .email("test@example.com")
                .password("StrongPassword123!")
                .build();

        // When - Tentative d'inscription
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            authService.register(request);
        });

        // Then - Vérifier le message d'erreur
        assertTrue(exception.getMessage().contains("Les nouvelles inscriptions sont temporairement désactivées"));
    }
}