package com.sellam.store.profile.dto;

import com.sellam.store.profile.models.ThemePreferenceEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ProfileDTO
{

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ThemeUpdateRequest
    {
        private ThemePreferenceEnum themePreference;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProfileResponse
    {
        private String name;
        private String phoneNumber;
        private String email;
        private String profilePictureUrl;
        private ThemePreferenceEnum themePreference;
    }
}
