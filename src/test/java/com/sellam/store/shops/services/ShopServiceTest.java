package com.sellam.store.shops.services;

import com.sellam.store.accounts.models.AccountEntity;
import com.sellam.store.accounts.repositories.AccountRepository;
import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.shops.dto.ShopDTO;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ShopServiceTest {

    private ShopRepository shopRepository;
    private AccountRepository accountRepository;
    private ShopService shopService;

    private UUID accountId;
    private UUID shopId;
    private AccountEntity account;

    @BeforeEach
    void setUp() {
        shopRepository = mock(ShopRepository.class);
        accountRepository = mock(AccountRepository.class);
        shopService = new ShopService(shopRepository, accountRepository);

        accountId = UUID.randomUUID();
        shopId = UUID.randomUUID();

        account = AccountEntity.builder()
                .id(accountId)
                .name("Alice")
                .phoneNumber("690000000")
                .build();
    }

    @Test
    void createShop_shouldCreateShopSuccessfully() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(shopRepository.save(any(ShopEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopDTO.ShopRequest request = ShopDTO.ShopRequest.builder()
                .name("Alimentation Générale Nlonkak")
                .address("Carrefour Nlonkak, Yaoundé")
                .phoneNumber("690000001")
                .taxpayerNumber("123456")
                .logoUrl("https://example.com/logo.jpg")
                .build();

        ShopDTO.ShopResponse response = shopService.createShop(request, accountId);

        assertNotNull(response);
        assertEquals("Alimentation Générale Nlonkak", response.getName());
        assertEquals("Carrefour Nlonkak, Yaoundé", response.getAddress());
        assertEquals("690000001", response.getPhoneNumber());
        assertEquals("123456", response.getTaxpayerNumber());
        assertEquals("https://example.com/logo.jpg", response.getLogoUrl());

        verify(shopRepository).save(any(ShopEntity.class));
    }

    @Test
    void createShop_shouldThrowExceptionWhenAccountNotFound() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        ShopDTO.ShopRequest request = ShopDTO.ShopRequest.builder()
                .name("Test Shop")
                .build();

        assertThrows(ResourceNotFoundException.class, () -> shopService.createShop(request, accountId));
    }

    @Test
    void createShop_shouldCreateShopWithMinimalData() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(shopRepository.save(any(ShopEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopDTO.ShopRequest request = ShopDTO.ShopRequest.builder()
                .name("Minimal Shop")
                .build();

        ShopDTO.ShopResponse response = shopService.createShop(request, accountId);

        assertNotNull(response);
        assertEquals("Minimal Shop", response.getName());
        assertNull(response.getAddress());
        assertNull(response.getPhoneNumber());

        verify(shopRepository).save(any(ShopEntity.class));
    }

    @Test
    void listShops_shouldReturnShopsForAccount() {
        ShopEntity shop1 = ShopEntity.builder()
                .id(UUID.randomUUID())
                .name("Boutique 1")
                .address("Address 1")
                .account(account)
                .build();

        ShopEntity shop2 = ShopEntity.builder()
                .id(UUID.randomUUID())
                .name("Boutique 2")
                .address("Address 2")
                .account(account)
                .build();

        when(shopRepository.findByAccount_Id(accountId)).thenReturn(Arrays.asList(shop1, shop2));

        List<ShopDTO.ShopResponse> shops = shopService.listShops(accountId);

        assertEquals(2, shops.size());
        assertEquals("Boutique 1", shops.get(0).getName());
        assertEquals("Boutique 2", shops.get(1).getName());
    }

    @Test
    void listShops_shouldReturnEmptyListWhenNoShops() {
        when(shopRepository.findByAccount_Id(accountId)).thenReturn(Arrays.asList());

        List<ShopDTO.ShopResponse> shops = shopService.listShops(accountId);

        assertTrue(shops.isEmpty());
    }

    @Test
    void getShopById_shouldReturnShop() {
        ShopEntity shop = ShopEntity.builder()
                .id(shopId)
                .name("Test Shop")
                .address("Test Address")
                .phoneNumber("690000001")
                .taxpayerNumber("123456")
                .logoUrl("https://example.com/logo.jpg")
                .autoPrintInvoices(true)
                .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));

        ShopDTO.ShopResponse response = shopService.getShopById(shopId);

        assertNotNull(response);
        assertEquals("Test Shop", response.getName());
        assertEquals("Test Address", response.getAddress());
        assertEquals("690000001", response.getPhoneNumber());
        assertEquals("123456", response.getTaxpayerNumber());
        assertEquals("https://example.com/logo.jpg", response.getLogoUrl());
        assertTrue(response.getAutoPrintInvoices());
    }

    @Test
    void getShopById_shouldThrowExceptionWhenNotFound() {
        when(shopRepository.findById(shopId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> shopService.getShopById(shopId));
    }

    @Test
    void updateSettings_shouldUpdateAutoPrintInvoices() {
        ShopEntity shop = ShopEntity.builder()
                .id(shopId)
                .name("Test Shop")
                .autoPrintInvoices(false)
                .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(shopRepository.save(any(ShopEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopDTO.ShopSettingsRequest request = ShopDTO.ShopSettingsRequest.builder()
                .autoPrintInvoices(true)
                .build();

        ShopDTO.ShopResponse response = shopService.updateSettings(shopId, request);

        assertNotNull(response);
        assertTrue(response.getAutoPrintInvoices());
        verify(shopRepository).save(shop);
    }

    @Test
    void updateSettings_shouldUpdateLogoUrl() {
        ShopEntity shop = ShopEntity.builder()
                .id(shopId)
                .name("Test Shop")
                .logoUrl(null)
                .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(shopRepository.save(any(ShopEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopDTO.ShopSettingsRequest request = ShopDTO.ShopSettingsRequest.builder()
                .logoUrl("https://example.com/new-logo.jpg")
                .build();

        ShopDTO.ShopResponse response = shopService.updateSettings(shopId, request);

        assertNotNull(response);
        assertEquals("https://example.com/new-logo.jpg", response.getLogoUrl());
        verify(shopRepository).save(shop);
    }

    @Test
    void updateSettings_shouldUpdatePhoneNumber() {
        ShopEntity shop = ShopEntity.builder()
                .id(shopId)
                .name("Test Shop")
                .phoneNumber("690000001")
                .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(shopRepository.save(any(ShopEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopDTO.ShopSettingsRequest request = ShopDTO.ShopSettingsRequest.builder()
                .phoneNumber("690000002")
                .build();

        ShopDTO.ShopResponse response = shopService.updateSettings(shopId, request);

        assertNotNull(response);
        assertEquals("690000002", response.getPhoneNumber());
        verify(shopRepository).save(shop);
    }

    @Test
    void updateSettings_shouldUpdateTaxpayerNumber() {
        ShopEntity shop = ShopEntity.builder()
                .id(shopId)
                .name("Test Shop")
                .taxpayerNumber("123456")
                .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(shopRepository.save(any(ShopEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopDTO.ShopSettingsRequest request = ShopDTO.ShopSettingsRequest.builder()
                .taxpayerNumber("654321")
                .build();

        ShopDTO.ShopResponse response = shopService.updateSettings(shopId, request);

        assertNotNull(response);
        assertEquals("654321", response.getTaxpayerNumber());
        verify(shopRepository).save(shop);
    }

    @Test
    void updateSettings_shouldUpdateMultipleSettings() {
        ShopEntity shop = ShopEntity.builder()
                .id(shopId)
                .name("Test Shop")
                .autoPrintInvoices(false)
                .logoUrl("https://example.com/old-logo.jpg")
                .phoneNumber("690000001")
                .taxpayerNumber("123456")
                .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(shopRepository.save(any(ShopEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopDTO.ShopSettingsRequest request = ShopDTO.ShopSettingsRequest.builder()
                .autoPrintInvoices(true)
                .logoUrl("https://example.com/new-logo.jpg")
                .phoneNumber("690000002")
                .taxpayerNumber("654321")
                .build();

        ShopDTO.ShopResponse response = shopService.updateSettings(shopId, request);

        assertNotNull(response);
        assertTrue(response.getAutoPrintInvoices());
        assertEquals("https://example.com/new-logo.jpg", response.getLogoUrl());
        assertEquals("690000002", response.getPhoneNumber());
        assertEquals("654321", response.getTaxpayerNumber());
        verify(shopRepository).save(shop);
    }

    @Test
    void updateSettings_shouldNotUpdateNullFields() {
        ShopEntity shop = ShopEntity.builder()
                .id(shopId)
                .name("Test Shop")
                .autoPrintInvoices(false)
                .logoUrl("https://example.com/logo.jpg")
                .phoneNumber("690000001")
                .taxpayerNumber("123456")
                .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(shopRepository.save(any(ShopEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopDTO.ShopSettingsRequest request = ShopDTO.ShopSettingsRequest.builder()
                .autoPrintInvoices(true)
                .build();

        ShopDTO.ShopResponse response = shopService.updateSettings(shopId, request);

        assertNotNull(response);
        assertTrue(response.getAutoPrintInvoices());
        assertEquals("https://example.com/logo.jpg", response.getLogoUrl()); // Should remain unchanged
        assertEquals("690000001", response.getPhoneNumber()); // Should remain unchanged
        assertEquals("123456", response.getTaxpayerNumber()); // Should remain unchanged
    }

    @Test
    void updateSettings_shouldThrowExceptionWhenShopNotFound() {
        when(shopRepository.findById(shopId)).thenReturn(Optional.empty());

        ShopDTO.ShopSettingsRequest request = ShopDTO.ShopSettingsRequest.builder()
                .autoPrintInvoices(true)
                .build();

        assertThrows(ResourceNotFoundException.class, () -> shopService.updateSettings(shopId, request));
    }
}