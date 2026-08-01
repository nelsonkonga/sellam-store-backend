package com.sellam.store.saletypes.services;

import com.sellam.store.saletypes.dto.SaleTypeDTO;
import com.sellam.store.saletypes.models.SaleTypeEntity;
import com.sellam.store.saletypes.repositories.SaleTypeRepository;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SaleTypeService {
    private final SaleTypeRepository saleTypeRepository;
    private final ShopRepository shopRepository;

    public SaleTypeService(SaleTypeRepository saleTypeRepository, ShopRepository shopRepository) {
        this.saleTypeRepository = saleTypeRepository;
        this.shopRepository = shopRepository;
    }

    public List<SaleTypeDTO.Response> listAvailableTypes(UUID shopId) {
        return saleTypeRepository.findByIsDefaultTrueOrShop_Id(shopId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SaleTypeDTO.Response createCustomType(UUID shopId, SaleTypeDTO.Request request) {
        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Boutique introuvable"));

        SaleTypeEntity entity = SaleTypeEntity.builder()
                .name(request.getName())
                .unitLabel(request.getUnitLabel())
                .isDefault(false)
                .shop(shop)
                .build();

        return toResponse(saleTypeRepository.save(entity));
    }

    private SaleTypeDTO.Response toResponse(SaleTypeEntity entity) {
        return SaleTypeDTO.Response.builder()
                .id(entity.getId())
                .name(entity.getName())
                .unitLabel(entity.getUnitLabel())
                .isDefault(entity.isDefault())
                .shopId(entity.getShop() != null ? entity.getShop().getId() : null)
                .build();
    }
}
