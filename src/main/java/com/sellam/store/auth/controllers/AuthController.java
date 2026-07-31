package com.sellam.store.auth.controllers;

import com.sellam.store.auth.services.AuthService;
import com.sellam.store.auth.dto.AuthDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController
{

    private final AuthService authService;

    public AuthController(AuthService authService)
    {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDTO.AuthOutput> register(@Valid @RequestBody AuthDTO.RegisterInput request)
    {
        AuthDTO.AuthOutput response = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDTO.AuthOutput> login(@Valid @RequestBody AuthDTO.LoginInput request)
    {
        AuthDTO.AuthOutput response = authService.login(request);

        return ResponseEntity.ok(response);
    }
}