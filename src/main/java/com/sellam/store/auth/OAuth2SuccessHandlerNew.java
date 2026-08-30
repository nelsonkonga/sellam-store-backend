package com.sellam.store.auth;

import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.repositories.PersonRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Handler OAuth2 adapté au nouveau modèle PersonEntity (Phase 1).
 * 
 * Différences avec OAuth2SuccessHandler legacy :
 * - Utilise PersonRepository au lieu de AccountRepository
 * - Utilise JwtProviderNew au lieu de JwtProvider
 * - Génère des tokens PERSON au lieu de ACCOUNT
 * - Bloque la création de nouveaux comptes OAuth2 (canary block)
 * - Redirige vers la page d'erreur OAuth2 si l'enregistrement est bloqué
 */
@Slf4j
@Component
@Profile("phase1")
@RequiredArgsConstructor
public class OAuth2SuccessHandlerNew implements AuthenticationSuccessHandler
{

    private final PersonRepository personRepository;
    private final JwtProviderNew jwtProviderNew;

    @Value("${app.registration.blocked:false}")
    private boolean registrationBlocked;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

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

        // Chercher une personne existante par email
        Optional<PersonEntity> existingPerson = personRepository.findByEmail(email);

        if (existingPerson.isEmpty())
        {
            // Blocage des nouvelles inscriptions OAuth2 (canary)
            if (registrationBlocked)
            {
                log.warn("OAuth2 registration blocked for email: {}", email);
                String errorUrl = frontendUrl + "/oauth-error?error=registration_blocked";
                response.sendRedirect(errorUrl);
                return;
            }

            // Créer une nouvelle personne (normalement bloqué en canary)
            PersonEntity newPerson = PersonEntity.builder()
                    .name(name)
                    .email(email)
                    .emailVerified(true)
                    .oauthProvider("google")
                    .oauthId(googleId)
                    .build();
            PersonEntity savedPerson = personRepository.save(newPerson);
            
            String token = jwtProviderNew.generateToken(savedPerson.getId(), "PERSON", null, savedPerson.getPhoneNumber());
            String redirectUrl = buildRedirectUrl(token, savedPerson.getId(), savedPerson.getName(), savedPerson.getEmail(), true);
            response.sendRedirect(redirectUrl);
            return;
        }

        // Personne existante : mettre à jour les infos OAuth2 si nécessaire
        PersonEntity person = existingPerson.get();
        
        if (person.getOauthProvider() == null)
        {
            person.setOauthProvider("google");
            person.setOauthId(googleId);
            person.setEmailVerified(true);
            personRepository.save(person);
        }

        String token = jwtProviderNew.generateToken(person.getId(), "PERSON", null, person.getPhoneNumber());
        String redirectUrl = buildRedirectUrl(token, person.getId(), person.getName(), person.getEmail(), person.isEmailVerified());
        response.sendRedirect(redirectUrl);
    }

    private String buildRedirectUrl(String token, UUID personId, String name, String email, boolean emailVerified)
    {
        return frontendUrl + "/oauth-callback?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&personId=" + personId
                + "&name=" + URLEncoder.encode(name != null ? name : "", StandardCharsets.UTF_8)
                + "&email=" + URLEncoder.encode(email != null ? email : "", StandardCharsets.UTF_8)
                + "&emailVerified=" + emailVerified;
    }
}
