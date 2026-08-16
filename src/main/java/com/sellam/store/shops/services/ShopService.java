package com.sellam.store.shops.services;

import com.sellam.store.accounts.models.AccountEntity;
import com.sellam.store.accounts.repositories.AccountRepository;
import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.shops.dto.ShopDTO;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@AllArgsConstructor
public class ShopService
{

    private final ShopRepository shopRepository;
    private final AccountRepository accountRepository;


    public ShopDTO.ShopResponse createShop(ShopDTO.ShopRequest request, UUID accountId)
    {

        AccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable"));

        ShopEntity shop = ShopEntity.builder()
                .name(request.getName())
                .address(request.getAddress())
                .logoUrl(request.getLogoUrl())
                .phoneNumber(request.getPhoneNumber())
                .taxpayerNumber(request.getTaxpayerNumber())
                .account(account)
                .build();

        ShopEntity newShop = shopRepository.save(shop);
        return toResponse(newShop);
    }

    public List<ShopDTO.ShopResponse> listShops(UUID accountId)
    {
        return shopRepository.findByAccount_Id(accountId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ShopDTO.ShopResponse getShopById(UUID shopId) {
        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique introuvable"));
        return toResponse(shop);
    }

    private ShopDTO.ShopResponse toResponse(ShopEntity shop)
    {
        return ShopDTO.ShopResponse.builder()
                .id(shop.getId())
                .name(shop.getName())
                .address(shop.getAddress())
                .logoUrl(shop.getLogoUrl())
                .phoneNumber(shop.getPhoneNumber())
                .taxpayerNumber(shop.getTaxpayerNumber())
                .autoPrintInvoices(shop.getAutoPrintInvoices())
                .build();
    }


    public ShopDTO.ShopResponse updateSettings(UUID shopId, ShopDTO.ShopSettingsRequest request)
    {
        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique introuvable"));
        if (request.getAutoPrintInvoices() != null)
        {
            shop.setAutoPrintInvoices(request.getAutoPrintInvoices());
        }
        if (request.getLogoUrl() != null)
        {
            shop.setLogoUrl(request.getLogoUrl());
        }
        if (request.getPhoneNumber() != null)
        {
            shop.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getTaxpayerNumber() != null)
        {
            shop.setTaxpayerNumber(request.getTaxpayerNumber());
        }
        return toResponse(shopRepository.save(shop));
    }
}