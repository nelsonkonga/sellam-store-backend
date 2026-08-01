package com.sellam.store.profile.controllers;

import com.sellam.store.profile.dto.ProfileDTO;
import com.sellam.store.profile.services.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
        UUID accountId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(profileService.getProfile(accountId));
    }

    @PutMapping("/theme")
    public ResponseEntity<Void> updateTheme(Authentication authentication, @RequestBody ProfileDTO.ThemeUpdateRequest request) {
        UUID accountId = (UUID) authentication.getPrincipal();
        profileService.updateThemePreference(accountId, request);
        return ResponseEntity.ok().build();
    }
}
