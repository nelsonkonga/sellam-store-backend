package com.sellam.store.profile.controllers;

import com.sellam.store.profile.dto.ProfileDTO;
import com.sellam.store.profile.services.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import com.sellam.store.common.security.AuthPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<ProfileDTO.ProfileResponse> getProfile(Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(profileService.getProfile(principal));
    }

    @PutMapping("/theme")
    public ResponseEntity<Void> updateTheme(Authentication authentication, @RequestBody ProfileDTO.ThemeUpdateRequest request) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        profileService.updateThemePreference(principal, request);
        return ResponseEntity.ok().build();
    }
}
