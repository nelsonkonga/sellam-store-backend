package com.sellam.store.auth.controllers;

import com.sellam.store.auth.services.AuthService;
import com.sellam.store.auth.dto.AuthDTO;
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

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDTO.AuthResponse register(@Valid @RequestBody AuthDTO.RegisterRequest request)
    {
        return authService.register(request);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthDTO.AuthResponse login(@Valid @RequestBody AuthDTO.LoginRequest request)
    {
        return authService.login(request);
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
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié");
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