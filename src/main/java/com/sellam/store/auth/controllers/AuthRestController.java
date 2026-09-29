package com.sellam.store.auth.controllers;

import com.sellam.store.auth.AuthCookieWriter;
import com.sellam.store.auth.services.AuthService;
import com.sellam.store.auth.dto.AuthDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import com.sellam.store.common.security.AuthPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;



@RestController
@AllArgsConstructor
@RequestMapping("/api/auth")

public class AuthRestController
{

    private final AuthService authService;
    private final AuthCookieWriter authCookieWriter;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDTO.AuthResponse register(@Valid @RequestBody AuthDTO.RegisterRequest request,
                                         HttpServletRequest httpRequest,
                                         HttpServletResponse httpResponse)
    {
        AuthDTO.AuthResponse response = authService.register(request);
        authCookieWriter.writeAccessCookie(httpRequest, httpResponse, response.getToken());
        return response;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthDTO.AuthResponse login(@Valid @RequestBody AuthDTO.LoginRequest request,
                                      HttpServletRequest httpRequest,
                                      HttpServletResponse httpResponse)
    {
        AuthDTO.AuthResponse response = authService.login(request);
        authCookieWriter.writeAccessCookie(httpRequest, httpResponse, response.getToken());
        return response;
    }

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public AuthDTO.AuthResponse me(Authentication authentication, HttpServletRequest httpRequest, HttpServletResponse httpResponse)
    {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal))
        {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expirée. Reconnectez-vous.");
        }
        AuthDTO.AuthResponse response = authService.resumeSession(principal.getId());
        authCookieWriter.writeAccessCookie(httpRequest, httpResponse, response.getToken());
        return response;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(Authentication authentication, HttpServletRequest httpRequest, HttpServletResponse httpResponse)
    {
        if (authentication != null && authentication.getPrincipal() instanceof AuthPrincipal principal)
        {
            authService.logout(principal.getId());
        }
        authCookieWriter.clearAccessCookie(httpRequest, httpResponse);
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.OK)
    public Void verifyEmail(@RequestParam String token)
    {
        authService.verifyEmail(token);
        return null;
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.OK)
    public Void resendVerification(Authentication authentication)
    {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifiÃƒÂ©");
        }

        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        authService.resendVerificationEmail(principal.getId());
        
        return null;
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.OK)
    public AuthDTO.ForgotPasswordResponse forgotPassword(@Valid @RequestBody AuthDTO.ForgotPasswordRequest request)
    {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.OK)
    public AuthDTO.ResetPasswordResponse resetPassword(@Valid @RequestBody AuthDTO.ResetPasswordRequest request)
    {
        return authService.resetPassword(request);
    }
}
