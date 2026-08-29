package com.sellam.store.auth;

import com.sellam.store.accounts.models.AccountEntity;
import com.sellam.store.accounts.repositories.AccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test pour valider le blocage technique des inscriptions OAuth2 pendant la fenêtre canary.
 */
@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerBlockTest
{

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oauthUser;

    @Test
    void onAuthenticationSuccess_WhenRegistrationDisabled_ShouldBlockNewAccountCreation()
    {
        // Given - Handler avec registration désactivée
        OAuth2SuccessHandler handler = new OAuth2SuccessHandler(accountRepository, jwtProvider);
        handler.setFrontendUrl("http://localhost:5173");
        handler.setRegistrationEnabled(false);

        // Mock OAuth2 user avec nouvel email
        when(authentication.getPrincipal()).thenReturn(oauthUser);
        when(oauthUser.getAttribute("email")).thenReturn("newuser@example.com");
        when(accountRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());

        // When - Tentative de connexion OAuth avec nouvel email
        Exception exception = assertThrows(RuntimeException.class, () -> {
            handler.onAuthenticationSuccess(request, response, authentication);
        });

        // Then - Vérifier le message d'erreur
        assertTrue(exception.getMessage().contains("Les nouvelles inscriptions sont temporairement désactivées"));
    }
}