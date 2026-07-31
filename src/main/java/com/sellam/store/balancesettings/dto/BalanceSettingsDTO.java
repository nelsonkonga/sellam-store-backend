package com.sellam.store.balancesettings.dto;

import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public class BalanceSettingsDTO
{

    @Data
    @AllArgsConstructor
    @Builder
    public static class SettingsRequest
    {
        @NonNull
        private DayOfWeek dayOfWeek;

        @NonNull
        private LocalTime balanceTime;

        private Integer reminderFrequencyHours;
    }


    @Data
    @AllArgsConstructor
    @Builder
    public static class SettingsResponse
    {
        private UUID id;

        private DayOfWeek dayOfWeek;

        private LocalTime balanceTime;

        private Integer reminderFrequencyHours;
    }
}