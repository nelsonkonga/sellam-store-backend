package com.sellam.store.auth;

import com.sellam.store.accounts.models.AccountEntity;
import com.sellam.store.accounts.repositories.AccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler
{

    private final AccountRepository accountRepository;
    private final JwtProvider jwtProvider;

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

        String redirectUrl = "http://localhost:5173/oauth-callback?token=" + token
                + "&accountId=" + account.getId()
                + "&name=" + account.getName()
                + "&email=" + (account.getEmail() != null ? account.getEmail() : "")
                + "&emailVerified=" + account.isEmailVerified();
        response.sendRedirect(redirectUrl); 
    }
}