package com.sellam.store.balancesettings.services;

import com.sellam.store.balancesettings.dto.BalanceSettingsDTO;
import com.sellam.store.balancesettings.models.BalanceSettingsEntity;
import com.sellam.store.balancesettings.repositories.BalanceSettingsRepository;
import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class BalanceSettingsService
{

    private final BalanceSettingsRepository balanceSettingsRepository;

    private final ShopRepository shopRepository;


    @Transactional
    public BalanceSettingsDTO.SettingsResponse saveSetting
            (
                    UUID shopId,
                    BalanceSettingsDTO.SettingsRequest request
            )
    {

        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique introuvable"));

        BalanceSettingsEntity setting = balanceSettingsRepository
                .findByShop_IdAndDayOfWeek(shopId, request.getDayOfWeek())
                .orElse(new BalanceSettingsEntity());

        setting.setShop(shop);
        setting.setDayOfWeek(request.getDayOfWeek());
        setting.setBalanceTime(request.getBalanceTime());
        setting.setReminderFrequencyHours(request.getReminderFrequencyHours());
        setting.setEnabled(request.isEnabled());
        setting.setOpeningTime(request.getOpeningTime());
        setting.setClosingTime(request.getClosingTime());

        BalanceSettingsEntity saved = balanceSettingsRepository.save(setting);

        return toResponse(saved);
    }

    public List<BalanceSettingsDTO.SettingsResponse> listSettings(UUID shopId)
    {
        return balanceSettingsRepository.findByShop_Id(shopId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


    private BalanceSettingsDTO.SettingsResponse toResponse(BalanceSettingsEntity setting)
    {
        return BalanceSettingsDTO.SettingsResponse.builder()
                .id(setting.getId())
                .dayOfWeek(setting.getDayOfWeek())
                .balanceTime(setting.getBalanceTime())
                .reminderFrequencyHours(setting.getReminderFrequencyHours())
                .enabled(setting.isEnabled())
                .openingTime(setting.getOpeningTime())
                .closingTime(setting.getClosingTime())
                .build();
    }
}