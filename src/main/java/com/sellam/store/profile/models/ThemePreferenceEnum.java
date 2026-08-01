package com.sellam.store.profile.models;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ThemePreferenceEnum
{
    LIGHT,
    DARK,
    SYSTEM;

    @JsonCreator
    public static ThemePreferenceEnum fromString(String value) {
        if (value == null) return null;
        return ThemePreferenceEnum.valueOf(value.toUpperCase());
    }
}
