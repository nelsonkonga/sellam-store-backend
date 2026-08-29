package com.sellam.store.identity.services;

import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.models.ShopMembershipEntity;
import com.sellam.store.identity.models.SystemRoleEnum;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.identity.repositories.ShopMembershipRepository;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import com.sellam.store.users.models.PermissionEnum;
import com.sellam.store.users.models.RoleEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest
{

    @Mock
    private PersonRepository personRepository;

    @Mock
    private ShopMembershipRepository shopMembershipRepository;

    @Mock
    private ShopRepository shopRepository;

    @InjectMocks
    private IdentityService identityService;

    private UUID personId;
    private UUID shopId;
    private PersonEntity person;
    private ShopEntity shop;
    private ShopMembershipEntity membership;

    @BeforeEach
    void setUp()
    {
        personId = UUID.randomUUID();
        shopId = UUID.randomUUID();

        person = PersonEntity.builder()
                .id(personId)
                .name("Test Person")
                .phoneNumber("+33612345678")
                .email("test@example.com")
                .build();

        shop = ShopEntity.builder()
                .id(shopId)
                .name("Test Shop")
                .build();

        membership = ShopMembershipEntity.builder()
                .id(UUID.randomUUID())
                .person(person)
                .shop(shop)
                .role(RoleEnum.MANAGER)
                .active(true)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getActiveMemberships_ShouldReturnOnlyActiveMemberships()
    {
        // Given
        ShopMembershipEntity inactiveMembership = ShopMembershipEntity.builder()
                .id(UUID.randomUUID())
                .person(person)
                .shop(shop)
                .role(RoleEnum.CASHIER)
                .active(false)
                .joinedAt(LocalDateTime.now())
                .build();

        when(shopMembershipRepository.findByPersonId(personId))
                .thenReturn(List.of(membership, inactiveMembership));

        // When
        List<ShopMembershipEntity> result = identityService.getActiveMemberships(personId);

        // Then
        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
        assertEquals(RoleEnum.MANAGER, result.get(0).getRole());
    }

    @Test
    void canChangePhoneNumber_WithPlatformAdmin_ShouldReturnTrue()
    {
        // Given
        person.setSystemRole(SystemRoleEnum.PLATFORM_ADMIN);
        person.setLastPhoneChangeAt(LocalDateTime.now()); // Récemment changé

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));

        // When
        boolean result = identityService.canChangePhoneNumber(personId);

        // Then
        assertTrue(result);
    }

    @Test
    void canChangePhoneNumber_WithinThreeMonths_ShouldReturnFalse()
    {
        // Given
        person.setLastPhoneChangeAt(LocalDateTime.now().minusMonths(1)); // Il y a 1 mois

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));

        // When
        boolean result = identityService.canChangePhoneNumber(personId);

        // Then
        assertFalse(result);
    }

    @Test
    void canChangePhoneNumber_AfterThreeMonths_ShouldReturnTrue()
    {
        // Given
        person.setLastPhoneChangeAt(LocalDateTime.now().minusMonths(4)); // Il y a 4 mois

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));

        // When
        boolean result = identityService.canChangePhoneNumber(personId);

        // Then
        assertTrue(result);
    }

    @Test
    void canChangePhoneNumber_FirstChange_ShouldReturnTrue()
    {
        // Given
        person.setLastPhoneChangeAt(null); // Jamais changé

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));

        // When
        boolean result = identityService.canChangePhoneNumber(personId);

        // Then
        assertTrue(result);
    }

    @Test
    void changePhoneNumber_WithValidConditions_ShouldSucceed()
    {
        // Given
        String newPhone = "+33698765432";
        person.setLastPhoneChangeAt(LocalDateTime.now().minusMonths(4));

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(personRepository.findByPhoneNumber(newPhone)).thenReturn(Optional.empty());
        when(personRepository.save(any(PersonEntity.class))).thenReturn(person);

        // When
        identityService.changePhoneNumber(personId, newPhone, false);

        // Then
        assertEquals(newPhone, person.getPhoneNumber());
        assertNotNull(person.getLastPhoneChangeAt());
        verify(personRepository).save(person);
    }

    @Test
    void changePhoneNumber_WithDuplicatePhone_ShouldThrowException()
    {
        // Given
        String newPhone = "+33698765432";
        PersonEntity otherPerson = PersonEntity.builder()
                .id(UUID.randomUUID())
                .phoneNumber(newPhone)
                .build();

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(personRepository.findByPhoneNumber(newPhone)).thenReturn(Optional.of(otherPerson));

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> identityService.changePhoneNumber(personId, newPhone, false));
    }

    @Test
    void changePhoneNumber_WithinThreeMonths_ShouldThrowException()
    {
        // Given
        String newPhone = "+33698765432";
        person.setLastPhoneChangeAt(LocalDateTime.now().minusMonths(1));

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> identityService.changePhoneNumber(personId, newPhone, false));
    }

    @Test
    void changePhoneNumber_WithAdminOverride_ShouldBypassTimeLimit()
    {
        // Given
        String newPhone = "+33698765432";
        person.setSystemRole(SystemRoleEnum.PLATFORM_ADMIN);
        person.setLastPhoneChangeAt(LocalDateTime.now()); // Récemment changé

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(personRepository.findByPhoneNumber(newPhone)).thenReturn(Optional.empty());
        when(personRepository.save(any(PersonEntity.class))).thenReturn(person);

        // When
        identityService.changePhoneNumber(personId, newPhone, true);

        // Then
        assertEquals(newPhone, person.getPhoneNumber());
        verify(personRepository).save(person);
    }

    @Test
    void createMembership_WithValidData_ShouldSucceed()
    {
        // Given
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(shopMembershipRepository.findActiveMembership(personId, shopId))
                .thenReturn(Optional.empty());
        when(shopMembershipRepository.save(any(ShopMembershipEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ShopMembershipEntity result = identityService.createMembership(personId, shopId, RoleEnum.CASHIER);

        // Then
        assertNotNull(result);
        assertEquals(person, result.getPerson());
        assertEquals(shop, result.getShop());
        assertEquals(RoleEnum.CASHIER, result.getRole()); // Vérifie que le rôle passé en paramètre est bien utilisé
        assertTrue(result.isActive());
        verify(shopMembershipRepository).save(any(ShopMembershipEntity.class));
    }

    @Test
    void createMembership_WithExistingMembership_ShouldThrowException()
    {
        // Given
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(shopMembershipRepository.findActiveMembership(personId, shopId))
                .thenReturn(Optional.of(membership));

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> identityService.createMembership(personId, shopId, RoleEnum.CASHIER));
    }

    @Test
    void createMembership_WithNonExistentPerson_ShouldThrowException()
    {
        // Given
        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> identityService.createMembership(personId, shopId, RoleEnum.CASHIER));
    }

    @Test
    void hasAccessToShop_WithActiveMembership_ShouldReturnTrue()
    {
        // Given
        when(shopMembershipRepository.findActiveMembership(personId, shopId))
                .thenReturn(Optional.of(membership));

        // When
        boolean result = identityService.hasAccessToShop(personId, shopId);

        // Then
        assertTrue(result);
    }

    @Test
    void hasAccessToShop_WithNoMembership_ShouldReturnFalse()
    {
        // Given
        when(shopMembershipRepository.findActiveMembership(personId, shopId))
                .thenReturn(Optional.empty());

        // When
        boolean result = identityService.hasAccessToShop(personId, shopId);

        // Then
        assertFalse(result);
    }
}
