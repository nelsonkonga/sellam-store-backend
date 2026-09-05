package com.sellam.store.profile.controllers;

import com.sellam.store.profile.dto.ProfileDTO;
import com.sellam.store.profile.services.ProfileService;
import com.sellam.store.shops.services.SupabaseStorageService;
import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.common.exception.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import com.sellam.store.common.security.AuthPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/profile")
public class ProfileRestController
{

    private final ProfileService profileService;
    private final SupabaseStorageService supabaseStorageService;
    private final PersonRepository personRepository;


    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ProfileDTO.ProfileResponse getProfile(Authentication authentication)
    {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié");
        }
        return profileService.getProfile(principal);
    }

    @PutMapping("/theme")
    @ResponseStatus(HttpStatus.OK)
    public Void updateTheme(Authentication authentication,
                            @RequestBody ProfileDTO.ThemeUpdateRequest request
                            )
    {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        if (principal == null)
        {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié");
        }
        profileService.updateThemePreference(principal, request);
        return null;
    }

    @PutMapping(value = "/picture", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> uploadProfilePicture(Authentication authentication,
                                                     @RequestParam("picture") MultipartFile file)
    {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        if (principal == null)
        {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié");
        }
        
        UUID personId = principal.getId();
        String pictureUrl = supabaseStorageService.uploadProfilePicture(personId, file);
        
        PersonEntity person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Personne introuvable"));
        person.setProfilePictureUrl(pictureUrl);
        personRepository.save(person);
        
        return Map.of("profilePictureUrl", pictureUrl);
    }
}
