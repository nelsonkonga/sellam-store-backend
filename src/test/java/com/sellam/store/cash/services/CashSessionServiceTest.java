package com.sellam.store.cash.services;

import com.sellam.store.cash.dto.CashDTO;
import com.sellam.store.cash.models.*;
import com.sellam.store.cash.repositories.*;
import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.invoices.models.InvoiceEntity;
import com.sellam.store.shops.models.ShopEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashSessionServiceTest {

    @Mock private CashRegisterRepository registerRepository;
    @Mock private CashRegisterSessionRepository sessionRepository;
    @Mock private CashMovementRepository movementRepository;
    @Mock private PersonRepository personRepository;

    @InjectMocks
    private CashSessionService cashSessionService;

    private PersonEntity personA;
    private PersonEntity personB;
    private CashRegisterEntity register;
    private ShopEntity shop;

    @BeforeEach
    void setUp() {
        shop = new ShopEntity();
        shop.setId(UUID.randomUUID());

        personA = new PersonEntity();
        personA.setId(UUID.randomUUID());
        personA.setName("Alice");

        personB = new PersonEntity();
        personB.setId(UUID.randomUUID());
        personB.setName("Bob");

        register = CashRegisterEntity.builder()
                .id(UUID.randomUUID())
                .shop(shop)
                .label("Caisse 1")
                .active(true)
                .build();
    }

    @Test
    void testOpenSession_NegativeAmount_ThrowsException() {
        CashDTO.OpenSessionRequest request = new CashDTO.OpenSessionRequest(BigDecimal.valueOf(-100));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cashSessionService.openSession(register.getId(), personA.getId(), request));

        assertTrue(exception.getMessage().contains("ne peut pas être négatif"));
    }

    @Test
    void testCloseSession_NegativeAmount_ThrowsException() {
        CashDTO.CloseSessionRequest request = new CashDTO.CloseSessionRequest(BigDecimal.valueOf(-50));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cashSessionService.closeSession(UUID.randomUUID(), personA.getId(), request));

        assertTrue(exception.getMessage().contains("ne peut pas être négatif"));
    }

    @Test
    void testHandover_NegativeResult_ThrowsException() {
        CashRegisterSessionEntity sessionA = CashRegisterSessionEntity.builder()
                .id(UUID.randomUUID())
                .register(register)
                .openedBy(personA)
                .status(CashSessionStatusEnum.PENDING_HANDOVER_CLOSURE)
                .openingCashAmount(BigDecimal.valueOf(1000))
                .build();

        CashRegisterSessionEntity sessionB = CashRegisterSessionEntity.builder()
                .id(UUID.randomUUID())
                .register(register)
                .openedBy(personB)
                .status(CashSessionStatusEnum.PENDING_INITIAL_CASH)
                .openingCashAmount(BigDecimal.ZERO)
                .build();

        when(sessionRepository.findById(sessionA.getId())).thenReturn(Optional.of(sessionA));
        when(sessionRepository.findById(sessionB.getId())).thenReturn(Optional.of(sessionB));
        
        // Simuler des ventes de B = 5000
        when(movementRepository.computeNetMovementsUntil(eq(sessionB.getId()), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.valueOf(5000));

        // Comptage par B = 4000. Déclaration A = 4000 - 5000 = -1000.
        CashDTO.HandoverCountRequest request = new CashDTO.HandoverCountRequest(
                BigDecimal.valueOf(4000), LocalDateTime.now());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> cashSessionService.resolveHandover(sessionA.getId(), sessionB.getId(), personB.getId(), request));

        assertTrue(exception.getMessage().contains("produit un montant négatif (-1000 FCFA)"));
    }

    @Test
    void testHandover_Success() {
        CashRegisterSessionEntity sessionA = CashRegisterSessionEntity.builder()
                .id(UUID.randomUUID())
                .register(register)
                .openedBy(personA)
                .status(CashSessionStatusEnum.PENDING_HANDOVER_CLOSURE)
                .openingCashAmount(BigDecimal.valueOf(1000))
                .build();

        CashRegisterSessionEntity sessionB = CashRegisterSessionEntity.builder()
                .id(UUID.randomUUID())
                .register(register)
                .openedBy(personB)
                .status(CashSessionStatusEnum.PENDING_INITIAL_CASH)
                .openingCashAmount(BigDecimal.ZERO)
                .build();

        when(sessionRepository.findById(sessionA.getId())).thenReturn(Optional.of(sessionA));
        when(sessionRepository.findById(sessionB.getId())).thenReturn(Optional.of(sessionB));
        when(personRepository.findById(personB.getId())).thenReturn(Optional.of(personB));

        // Mouvements de B = 5000
        when(movementRepository.computeNetMovementsUntil(eq(sessionB.getId()), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.valueOf(5000));
        
        // Mouvements de A = 2000
        when(movementRepository.computeNetMovements(sessionA.getId())).thenReturn(BigDecimal.valueOf(2000));

        // Comptage par B = 9000. Déclaration A = 9000 - 5000 = 4000. 
        // Computed A = 1000 (opening) + 2000 (net) = 3000.
        // Ecart A = 4000 - 3000 = +1000.
        CashDTO.HandoverCountRequest request = new CashDTO.HandoverCountRequest(
                BigDecimal.valueOf(9000), LocalDateTime.now());

        CashDTO.SessionSummary result = cashSessionService.resolveHandover(sessionA.getId(), sessionB.getId(), personB.getId(), request);

        assertEquals(CashSessionStatusEnum.OPEN, result.getStatus());
        assertEquals(BigDecimal.valueOf(4000), result.getOpeningCashAmount());
        
        assertEquals(CashSessionStatusEnum.CLOSED_BY_SYSTEM, sessionA.getStatus());
        assertEquals(BigDecimal.valueOf(4000), sessionA.getClosingDeclaredAmount());
        assertEquals(BigDecimal.valueOf(3000), sessionA.getClosingComputedAmount());
        assertEquals(BigDecimal.valueOf(1000), sessionA.getDiscrepancy());
    }

    @Test
    void testAutoOpenOnCashSale_WhenNoSession() {
        InvoiceEntity invoice = InvoiceEntity.builder()
                .id(UUID.randomUUID())
                .totalAmount(BigDecimal.valueOf(5000))
                .build();

        when(registerRepository.findFirstByShopIdAndActiveTrue(shop.getId())).thenReturn(Optional.of(register));
        when(personRepository.findById(personA.getId())).thenReturn(Optional.of(personA));
        
        // Aucune session active
        when(sessionRepository.findActiveSessionForUpdate(eq(register.getId()), anyList())).thenReturn(Optional.empty());
        
        // Comportement de la sauvegarde de la nouvelle session
        when(sessionRepository.saveAndFlush(any(CashRegisterSessionEntity.class))).thenAnswer(i -> {
            CashRegisterSessionEntity s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        cashSessionService.recordCashSale(shop.getId(), personA.getId(), invoice);

        // Vérifie qu'une nouvelle session a été ouverte
        verify(sessionRepository).saveAndFlush(argThat(s -> 
            s.getStatus() == CashSessionStatusEnum.PENDING_INITIAL_CASH && 
            s.getOpeningCashAmount().compareTo(BigDecimal.ZERO) == 0 &&
            s.getOpenedBy().getId().equals(personA.getId())
        ));

        // Vérifie que le mouvement a été enregistré
        verify(movementRepository).save(argThat(m -> 
            m.getType() == CashMovementTypeEnum.VENTE_CASH &&
            m.getAmount().compareTo(BigDecimal.valueOf(5000)) == 0
        ));
    }

    @Test
    void testAutoCloseEndOfDayUsesSingleBatchQuery() {
        CashRegisterSessionEntity openSession = CashRegisterSessionEntity.builder()
                .id(UUID.randomUUID())
                .register(register)
                .status(CashSessionStatusEnum.OPEN)
                .openingCashAmount(BigDecimal.valueOf(10000))
                .build();

        when(sessionRepository.findByStatusIn(anyList()))
                .thenReturn(List.of(openSession));
        when(movementRepository.computeNetMovements(openSession.getId()))
                .thenReturn(BigDecimal.valueOf(5000));

        cashSessionService.autoCloseEndOfDay();

        verify(sessionRepository, times(1)).findByStatusIn(anyList());
        verify(registerRepository, never()).findAll();
        verify(sessionRepository, times(1)).save(argThat(s ->
                s.getStatus() == CashSessionStatusEnum.CLOSED_BY_SYSTEM &&
                s.getClosingComputedAmount().compareTo(BigDecimal.valueOf(15000)) == 0
        ));
    }
}
