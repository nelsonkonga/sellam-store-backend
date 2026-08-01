package com.sellam.store.profile.services;

import com.sellam.store.accounts.models.AccountEntity;
import com.sellam.store.accounts.repositories.AccountRepository;
import com.sellam.store.profile.dto.ProfileDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProfileService {

    private final AccountRepository accountRepository;

    public ProfileService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public ProfileDTO.ProfileResponse getProfile(UUID accountId) {
        AccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        return ProfileDTO.ProfileResponse.builder()
                .name(account.getName())
                .phoneNumber(account.getPhoneNumber())
                .email(account.getEmail())
                .profilePictureUrl(account.getProfilePictureUrl())
                .themePreference(account.getThemePreference())
                .build();
    }

    @Transactional
    public void updateThemePreference(UUID accountId, ProfileDTO.ThemeUpdateRequest request) {
        AccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        account.setThemePreference(request.getThemePreference());
        accountRepository.save(account);
    }
}
