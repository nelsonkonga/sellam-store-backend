package com.sellam.store.profile.services;

import com.sellam.store.accounts.models.AccountEntity;
import com.sellam.store.accounts.repositories.AccountRepository;
import com.sellam.store.users.models.UserEntity;
import com.sellam.store.users.repositories.UserRepository;
import com.sellam.store.profile.dto.ProfileDTO;
import com.sellam.store.common.security.AuthPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public ProfileService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public ProfileDTO.ProfileResponse getProfile(AuthPrincipal principal) {
        if ("USER".equals(principal.getUserType())) {
            UserEntity user = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            return ProfileDTO.ProfileResponse.builder()
                    .name(user.getName())
                    .phoneNumber(user.getPhoneNumber())
                    .profilePictureUrl(user.getProfilePictureUrl())
                    .themePreference(user.getThemePreference())
                    .build();
        } else {
            AccountEntity account = accountRepository.findById(principal.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));
            return ProfileDTO.ProfileResponse.builder()
                    .name(account.getName())
                    .phoneNumber(account.getPhoneNumber())
                    .email(account.getEmail())
                    .profilePictureUrl(account.getProfilePictureUrl())
                    .themePreference(account.getThemePreference())
                    .build();
        }
    }

    @Transactional
    public void updateThemePreference(AuthPrincipal principal, ProfileDTO.ThemeUpdateRequest request) {
        if ("USER".equals(principal.getUserType())) {
            UserEntity user = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            user.setThemePreference(request.getThemePreference());
            userRepository.save(user);
        } else {
            AccountEntity account = accountRepository.findById(principal.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));
            account.setThemePreference(request.getThemePreference());
            accountRepository.save(account);
        }
    }
}
