package com.sellam.store.saletypes.services;

import com.sellam.store.saletypes.dto.SaleTypeDTO;
import com.sellam.store.saletypes.models.SaleTypeEntity;
import com.sellam.store.saletypes.repositories.SaleTypeRepository;
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
public class SaleTypeService
{
    private final SaleTypeRepository saleTypeRepository;
    private final ShopRepository shopRepository;


    public List<SaleTypeDTO.SaleTypeResponse> listAvailableTypes(UUID shopId)
    {
        return saleTypeRepository.findByIsDefaultTrueOrShop_Id(shopId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SaleTypeDTO.SaleTypeResponse createCustomType(UUID shopId,
                                                         SaleTypeDTO.SaleTypeRequest saleTypeRequest
                                                            )
    {
        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Boutique introuvable"));

        SaleTypeEntity entity = SaleTypeEntity.builder()
                .name(saleTypeRequest.getName())
                .unitLabel(saleTypeRequest.getUnitLabel())
                .isDefault(false)
                .shop(shop)
                .build();

        return toResponse(saleTypeRepository.save(entity));
    }

    private SaleTypeDTO.SaleTypeResponse toResponse(SaleTypeEntity entity)
    {
        return SaleTypeDTO.SaleTypeResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .unitLabel(entity.getUnitLabel())
                .isDefault(entity.isDefault())
                .shopId(entity.getShop() != null ? entity.getShop().getId() : null)
                .build();
    }
}
