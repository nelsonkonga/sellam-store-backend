package com.sellam.store.shops.services;

import com.sellam.store.accounts.models.AccountEntity;
import com.sellam.store.accounts.repositories.AccountRepository;
import com.sellam.store.shops.dto.ShopDTO;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class ShopService
{

    private final ShopRepository shopRepository;
    private final AccountRepository accountRepository;

    public ShopService(ShopRepository shopRepository, AccountRepository accountRepository)
    {
        this.shopRepository = shopRepository;
        this.accountRepository = accountRepository;
    }

    public ShopDTO.ShopResponse createShop(ShopDTO.ShopRequest request, UUID accountId)
    {

        AccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable"));

        ShopEntity shop = ShopEntity.builder()
                                    .name(request.getName())
                                    .address(request.getAddress())
                                    .logoUrl(request.getLogoUrl())
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

    private ShopDTO.ShopResponse toResponse(ShopEntity shop)
    {
        return new ShopDTO.ShopResponse(
                shop.getId(),
                shop.getName(),
                shop.getAddress(),
                shop.getLogoUrl()
        );
    }
}
