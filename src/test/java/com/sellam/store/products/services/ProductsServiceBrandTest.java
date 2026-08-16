package com.sellam.store.products.services;

import com.sellam.store.products.dto.ProductsDTO;
import com.sellam.store.products.models.ProductEntity;
import com.sellam.store.products.repositories.ProductsRepository;
import com.sellam.store.saletypes.models.SaleTypeEntity;
import com.sellam.store.saletypes.repositories.SaleTypeRepository;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductsServiceBrandTest {

    @Test
    void createProduct_shouldPersistBrand() {
        ProductsRepository productsRepository = mock(ProductsRepository.class);
        ShopRepository shopRepository = mock(ShopRepository.class);
        SaleTypeRepository saleTypeRepository = mock(SaleTypeRepository.class);

        UUID shopId = UUID.randomUUID();
        UUID saleTypeId = UUID.randomUUID();

        ShopEntity shop = ShopEntity.builder().id(shopId).name("Boutique Test").build();
        SaleTypeEntity saleType = SaleTypeEntity.builder().id(saleTypeId).name("Unité").unitLabel("kg").build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(saleTypeRepository.findById(saleTypeId)).thenReturn(Optional.of(saleType));
        when(productsRepository.findByShop_IdAndNameAndCategory(shopId, "Riz", "Épicerie")).thenReturn(null);
        when(productsRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductsService service = new ProductsService(productsRepository, shopRepository, saleTypeRepository);

        ProductsDTO.ProductResponse response = service.createProduct(
                ProductsDTO.ProductRequest.builder()
                        .name("Riz")
                        .barcode("123")
                        .brand("Moulin du Nord")
                        .saleTypeId(saleTypeId)
                        .purchasePrice(BigDecimal.valueOf(250))
                        .sellingPrice(BigDecimal.valueOf(500))
                        .stockQuantity(BigDecimal.valueOf(10))
                        .alertThreshold(BigDecimal.valueOf(2))
                        .category("Épicerie")
                        .shopId(shopId)
                        .build()
        );

        assertEquals("Moulin du Nord", response.getBrand());
    }
}
