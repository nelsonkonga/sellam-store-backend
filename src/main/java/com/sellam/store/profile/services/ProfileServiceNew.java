package com.sellam.store.profile.services;

import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.profile.dto.ProfileDTO;
import com.sellam.store.common.security.AuthPrincipal;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Profile;

/**
 * Service de profil adapté au nouveau modèle PersonEntity.
 * 
 * Changements par rapport à l'ancien ProfileService :
 * - Utilise PersonRepository au lieu de AccountRepository/UserRepository
 * - Plus de distinction entre ACCOUNT et USER : une seule entité PersonEntity
 * - Compatible avec le frontend via AuthPrincipal
 */
@Service
@Profile("phase1")
@AllArgsConstructor
public class ProfileServiceNew
{

    private final PersonRepository personRepository;

    public ProfileDTO.ProfileResponse getProfile(AuthPrincipal principal)
    {
        PersonEntity person = personRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Personne introuvable"));

        return ProfileDTO.ProfileResponse.builder()
                .name(person.getName())
                .phoneNumber(person.getPhoneNumber())
                .email(person.getEmail())
                .profilePictureUrl(person.getProfilePictureUrl())
                .themePreference(person.getThemePreference())
                .build();
    }

    @Transactional
    public void updateThemePreference(AuthPrincipal principal,
                                      ProfileDTO.ThemeUpdateRequest request
                                        )
    {
        PersonEntity person = personRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Personne introuvable"));

        person.setThemePreference(request.getThemePreference());
        personRepository.save(person);
    }
}
