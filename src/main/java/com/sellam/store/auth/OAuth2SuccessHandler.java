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


import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Handler OAuth2 adaptÃ© au nouveau modÃ¨le PersonEntity (Phase 1).
 * 
 * DiffÃ©rences avec OAuth2SuccessHandler legacy :
 * - Utilise PersonRepository au lieu de AccountRepository
 * - Utilise JwtProvider au lieu de JwtProvider
 * - GÃ©nÃ¨re des tokens PERSON au lieu de ACCOUNT
 * - Bloque la crÃ©ation de nouveaux comptes OAuth2 (canary block)
 * - Redirige vers la page d'erreur OAuth2 si l'enregistrement est bloquÃ©
 */
@Slf4j
@Component

@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler
{

    private final PersonRepository personRepository;
    private final JwtProvider JwtProvider;

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

            // CrÃ©er une nouvelle personne (normalement bloquÃ© en canary)
            PersonEntity newPerson = PersonEntity.builder()
                    .name(name)
                    .email(email)
                    .emailVerified(true)
                    .oauthProvider("google")
                    .oauthId(googleId)
                    .build();
            PersonEntity savedPerson = personRepository.save(newPerson);
            
            String token = JwtProvider.generateToken(savedPerson.getId(), savedPerson.getName(), "PERSON", null, savedPerson.getPhoneNumber());
            String redirectUrl = buildRedirectUrl(token, savedPerson.getId(), savedPerson.getName(), savedPerson.getEmail(), true);
            response.sendRedirect(redirectUrl);
            return;
        }

        // Personne existante : mettre Ã  jour les infos OAuth2 si nÃ©cessaire
        PersonEntity person = existingPerson.get();
        
        if (person.getOauthProvider() == null)
        {
            person.setOauthProvider("google");
            person.setOauthId(googleId);
            person.setEmailVerified(true);
            personRepository.save(person);
        }

        String token = JwtProvider.generateToken(person.getId(), person.getName(), "PERSON", null, person.getPhoneNumber());
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
