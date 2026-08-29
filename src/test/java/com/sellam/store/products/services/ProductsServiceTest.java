package com.sellam.store.products.services;

import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.products.dto.ProductsDTO;
import com.sellam.store.products.models.ProductEntity;
import com.sellam.store.products.models.exception.ProductsException;
import com.sellam.store.products.repositories.ProductsRepository;
import com.sellam.store.saletypes.models.SaleTypeEntity;
import com.sellam.store.saletypes.repositories.SaleTypeRepository;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductsServiceTest {

    private ProductsRepository productsRepository;
    private ShopRepository shopRepository;
    private SaleTypeRepository saleTypeRepository;
    private ProductsService productsService;

    private UUID shopId;
    private UUID saleTypeId;
    private UUID productId;
    private ShopEntity shop;
    private SaleTypeEntity saleType;

    @BeforeEach
    void setUp() {
        productsRepository = mock(ProductsRepository.class);
        shopRepository = mock(ShopRepository.class);
        saleTypeRepository = mock(SaleTypeRepository.class);
        productsService = new ProductsService(productsRepository, shopRepository, saleTypeRepository);

        shopId = UUID.randomUUID();
        saleTypeId = UUID.randomUUID();
        productId = UUID.randomUUID();

        shop = ShopEntity.builder()
                .id(shopId)
                .name("Boutique Test")
                .build();

        saleType = SaleTypeEntity.builder()
                .id(saleTypeId)
                .name("Unité")
                .unitLabel("kg")
                .build();
    }

    @Test
    void createProduct_shouldCreateProductSuccessfully() {
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(saleTypeRepository.findById(saleTypeId)).thenReturn(Optional.of(saleType));
        when(productsRepository.findByShop_IdAndNameAndCategory(shopId, "Riz", "Épicerie")).thenReturn(null);
        when(productsRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductsDTO.ProductRequest request = ProductsDTO.ProductRequest.builder()
                .name("Riz")
                .barcode("123456")
                .brand("Moulin du Nord")
                .saleTypeId(saleTypeId)
                .purchasePrice(BigDecimal.valueOf(250))
                .sellingPrice(BigDecimal.valueOf(500))
                .stockQuantity(BigDecimal.valueOf(10))
                .alertThreshold(BigDecimal.valueOf(2))
                .category("Épicerie")
                .shopId(shopId)
                .build();

        ProductsDTO.ProductResponse response = productsService.createProduct(request);

        assertNotNull(response);
        assertEquals("Riz", response.getName());
        assertEquals("Moulin du Nord", response.getBrand());
        assertEquals("Épicerie", response.getCategory());
        assertEquals(BigDecimal.valueOf(500), response.getSellingPrice());
        assertEquals(BigDecimal.valueOf(10), response.getStockQuantity());

        verify(productsRepository).save(any(ProductEntity.class));
    }

    @Test
    void createProduct_shouldThrowExceptionWhenNameIsEmpty() {
        ProductsDTO.ProductRequest request = ProductsDTO.ProductRequest.builder()
                .name("")
                .saleTypeId(saleTypeId)
                .purchasePrice(BigDecimal.valueOf(100))
                .sellingPrice(BigDecimal.valueOf(200))
                .stockQuantity(BigDecimal.valueOf(10))
                .alertThreshold(BigDecimal.valueOf(5))
                .shopId(shopId)
                .build();

        assertThrows(ProductsException.class, () -> productsService.createProduct(request));
    }

    @Test
    void createProduct_shouldThrowExceptionWhenProductAlreadyExists() {
        ProductEntity existingProduct = ProductEntity.builder()
                .id(productId)
                .name("Riz")
                .category("Épicerie")
                .shop(shop)
                .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(saleTypeRepository.findById(saleTypeId)).thenReturn(Optional.of(saleType));
        when(productsRepository.findByShop_IdAndNameAndCategory(shopId, "Riz", "Épicerie"))
                .thenReturn(existingProduct);

        ProductsDTO.ProductRequest request = ProductsDTO.ProductRequest.builder()
                .name("Riz")
                .category("Épicerie")
                .saleTypeId(saleTypeId)
                .purchasePrice(BigDecimal.valueOf(100))
                .sellingPrice(BigDecimal.valueOf(200))
                .stockQuantity(BigDecimal.valueOf(10))
                .alertThreshold(BigDecimal.valueOf(5))
                .shopId(shopId)
                .build();

        assertThrows(ProductsException.class, () -> productsService.createProduct(request));
    }

    @Test
    void createProduct_shouldThrowExceptionWhenShopNotFound() {
        when(shopRepository.findById(shopId)).thenReturn(Optional.empty());

        ProductsDTO.ProductRequest request = ProductsDTO.ProductRequest.builder()
                .name("Riz")
                .saleTypeId(saleTypeId)
                .purchasePrice(BigDecimal.valueOf(100))
                .sellingPrice(BigDecimal.valueOf(200))
                .stockQuantity(BigDecimal.valueOf(10))
                .alertThreshold(BigDecimal.valueOf(5))
                .shopId(shopId)
                .build();

        assertThrows(ResourceNotFoundException.class, () -> productsService.createProduct(request));
    }

    @Test
    void createProduct_shouldThrowExceptionWhenSaleTypeNotFound() {
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(saleTypeRepository.findById(saleTypeId)).thenReturn(Optional.empty());

        ProductsDTO.ProductRequest request = ProductsDTO.ProductRequest.builder()
                .name("Riz")
                .saleTypeId(saleTypeId)
                .purchasePrice(BigDecimal.valueOf(100))
                .sellingPrice(BigDecimal.valueOf(200))
                .stockQuantity(BigDecimal.valueOf(10))
                .alertThreshold(BigDecimal.valueOf(5))
                .shopId(shopId)
                .build();

        assertThrows(ResourceNotFoundException.class, () -> productsService.createProduct(request));
    }

    @Test
    void listProducts_shouldReturnProductsForShop() {
        ProductEntity product1 = ProductEntity.builder()
                .id(UUID.randomUUID())
                .name("Riz")
                .shop(shop)
                .saleType(saleType)
                .sellingPrice(BigDecimal.valueOf(500))
                .stockQuantity(BigDecimal.valueOf(10))
                .build();

        ProductEntity product2 = ProductEntity.builder()
                .id(UUID.randomUUID())
                .name("Pain")
                .shop(shop)
                .saleType(saleType)
                .sellingPrice(BigDecimal.valueOf(1500))
                .stockQuantity(BigDecimal.valueOf(20))
                .build();

        when(productsRepository.findByShop_Id(shopId)).thenReturn(Arrays.asList(product1, product2));

        List<ProductsDTO.ProductResponse> products = productsService.listProducts(shopId);

        assertEquals(2, products.size());
        assertEquals("Riz", products.get(0).getName());
        assertEquals("Pain", products.get(1).getName());
    }

    @Test
    void getProductById_shouldReturnProduct() {
        ProductEntity product = ProductEntity.builder()
                .id(productId)
                .name("Riz")
                .shop(shop)
                .saleType(saleType)
                .sellingPrice(BigDecimal.valueOf(500))
                .stockQuantity(BigDecimal.valueOf(10))
                .build();

        when(productsRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductsDTO.ProductResponse response = productsService.getProductById(productId);

        assertNotNull(response);
        assertEquals("Riz", response.getName());
        assertEquals(productId, response.getId());
    }

    @Test
    void getProductById_shouldThrowExceptionWhenNotFound() {
        when(productsRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productsService.getProductById(productId));
    }

    @Test
    void updateProduct_shouldUpdateProductSuccessfully() {
        ProductEntity product = ProductEntity.builder()
                .id(productId)
                .name("Riz")
                .shop(shop)
                .saleType(saleType)
                .sellingPrice(BigDecimal.valueOf(500))
                .stockQuantity(BigDecimal.valueOf(10))
                .build();

        when(productsRepository.findById(productId)).thenReturn(Optional.of(product));
        when(saleTypeRepository.findById(saleTypeId)).thenReturn(Optional.of(saleType));
        when(productsRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductsDTO.ProductRequest request = ProductsDTO.ProductRequest.builder()
                .name("Riz Premium")
                .sellingPrice(BigDecimal.valueOf(600))
                .stockQuantity(BigDecimal.valueOf(15))
                .purchasePrice(BigDecimal.valueOf(100))
                .alertThreshold(BigDecimal.valueOf(5))
                .saleTypeId(saleTypeId)
                .build();

        ProductsDTO.ProductResponse response = productsService.updateProduct(productId, request);

        assertNotNull(response);
        assertEquals("Riz Premium", response.getName());
        assertEquals(BigDecimal.valueOf(600), response.getSellingPrice());
        assertEquals(BigDecimal.valueOf(15), response.getStockQuantity());

        verify(productsRepository).save(product);
    }

    @Test
    void updateProduct_shouldThrowExceptionWhenProductNotFound() {
        when(productsRepository.findById(productId)).thenReturn(Optional.empty());

        ProductsDTO.ProductRequest request = ProductsDTO.ProductRequest.builder()
                .name("Riz")
                .saleTypeId(saleTypeId)
                .purchasePrice(BigDecimal.valueOf(100))
                .sellingPrice(BigDecimal.valueOf(200))
                .stockQuantity(BigDecimal.valueOf(10))
                .build();

        assertThrows(ResourceNotFoundException.class, () -> productsService.updateProduct(productId, request));
    }

    @Test
    void checkAlertThreshold_shouldReturnTrueWhenStockIsLow() {
        ProductEntity product = ProductEntity.builder()
                .id(productId)
                .stockQuantity(BigDecimal.valueOf(2))
                .alertThreshold(BigDecimal.valueOf(5))
                .build();

        when(productsRepository.findById(productId)).thenReturn(Optional.of(product));

        boolean result = productsService.checkAlertThreshold(productId);

        assertTrue(result);
    }

    @Test
    void checkAlertThreshold_shouldReturnFalseWhenStockIsSufficient() {
        ProductEntity product = ProductEntity.builder()
                .id(productId)
                .stockQuantity(BigDecimal.valueOf(10))
                .alertThreshold(BigDecimal.valueOf(5))
                .build();

        when(productsRepository.findById(productId)).thenReturn(Optional.of(product));

        boolean result = productsService.checkAlertThreshold(productId);

        assertFalse(result);
    }

    @Test
    void updateProductPictureUrl_shouldUpdatePictureUrl() {
        ProductEntity product = ProductEntity.builder()
                .id(productId)
                .name("Riz")
                .pictureUrl(null)
                .build();

        when(productsRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productsRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String newPictureUrl = "https://example.com/product.jpg";
        productsService.updateProductPictureUrl(productId, newPictureUrl);

        assertEquals(newPictureUrl, product.getPictureUrl());
        verify(productsRepository).save(product);
    }

    @Test
    void getProductByBarcode_shouldReturnProduct() {
        String barcode = "123456";
        ProductEntity product = ProductEntity.builder()
                .id(productId)
                .name("Riz")
                .barcode(barcode)
                .shop(shop)
                .saleType(saleType)
                .build();

        when(productsRepository.findByShop_IdAndBarcode(shopId, barcode)).thenReturn(Optional.of(product));

        ProductsDTO.ProductResponse response = productsService.getProductByBarcode(shopId, barcode);

        assertNotNull(response);
        assertEquals("Riz", response.getName());
        assertEquals(barcode, response.getBarcode());
    }

    @Test
    void getProductByBarcode_shouldThrowExceptionWhenNotFound() {
        when(productsRepository.findByShop_IdAndBarcode(shopId, "123456")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productsService.getProductByBarcode(shopId, "123456"));
    }
}