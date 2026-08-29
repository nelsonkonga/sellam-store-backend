package com.sellam.store.auth;

import com.sellam.store.accounts.models.AccountEntity;
import com.sellam.store.accounts.repositories.AccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler
{

    private final AccountRepository accountRepository;
    private final JwtProvider jwtProvider;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.registration.enabled}")
    private boolean registrationEnabled;

    // Setters pour les tests
    public void setFrontendUrl(String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    public void setRegistrationEnabled(boolean registrationEnabled) {
        this.registrationEnabled = registrationEnabled;
    }

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request,
                                        @NonNull HttpServletResponse response,
                                        @NonNull Authentication authentication) throws IOException
    {
        if (authentication == null || authentication.getPrincipal() == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentification manquante ou invalide");
            return;
        }

        if (!(authentication.getPrincipal() instanceof OAuth2User oauthUser)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Le principal n'est pas un utilisateur OAuth2");
            return;
        }

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String googleId = oauthUser.getAttribute("sub");

        // Vérifier si le compte existe déjà
        java.util.Optional<AccountEntity> existingAccount = accountRepository.findByEmail(email);
        
        if (existingAccount.isEmpty() && !registrationEnabled)
        {
            // BLOCAGE CANARY PHASE 1 : Rediriger vers le frontend avec message d'erreur
            String errorUrl = frontendUrl + "/oauth-error?error=registration_disabled&message=" 
                    + URLEncoder.encode("Les nouvelles inscriptions sont temporairement désactivées pour maintenance. Veuillez réessayer ultérieurement.", StandardCharsets.UTF_8);
            response.sendRedirect(errorUrl);
            return;
        }

        AccountEntity account = existingAccount.orElseGet(() -> {
            AccountEntity newAccount = AccountEntity.builder()
                    .name(name)
                    .email(email)
                    .emailVerified(true)
                    .oauthProvider("google")
                    .oauthId(googleId)
                    .build();
            return accountRepository.save(newAccount);
        });

        if (account.getOauthProvider() == null)
        {
            account.setOauthProvider("google");
            account.setOauthId(googleId);
            account.setEmailVerified(true);
            accountRepository.save(account);
        }

        String token = jwtProvider.generateToken(account.getId(), "ACCOUNT", null, account.getPhoneNumber());

        String redirectUrl = frontendUrl + "/oauth-callback?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&accountId=" + account.getId()
                + "&name=" + URLEncoder.encode(account.getName(), StandardCharsets.UTF_8)
                + "&email=" + URLEncoder.encode(account.getEmail() != null ? account.getEmail() : "", StandardCharsets.UTF_8)
                + "&emailVerified=" + account.isEmailVerified();
        response.sendRedirect(redirectUrl);
    }
}