package com.sellam.store.auth;

import com.sellam.store.accounts.models.AccountEntity;
import com.sellam.store.accounts.repositories.AccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler
{

    private final AccountRepository accountRepository;
    private final JwtProvider jwtProvider;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public OAuth2SuccessHandler(AccountRepository accountRepository, JwtProvider jwtProvider)
    {
        this.accountRepository = accountRepository;
        this.jwtProvider = jwtProvider;
    }

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request,
                                        @NonNull HttpServletResponse response,
                                        Authentication authentication) throws IOException
    {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        assert oauthUser != null;
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String googleId = oauthUser.getAttribute("sub");

        AccountEntity account = accountRepository.findByEmail(email)
                .orElseGet(() -> {
                    AccountEntity newAccount = AccountEntity.builder()
                            .name(name)
                            .email(email)
                            .emailVerified(true)
                            .oauthProvider("google")
                            .oauthId(googleId)
                            .build();
                    return accountRepository.save(newAccount);
                });

        if (account.getOauthProvider() == null) {
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