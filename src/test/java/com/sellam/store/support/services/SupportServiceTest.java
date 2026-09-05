package com.sellam.store.support.services;

import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.support.dto.SupportDTO;
import com.sellam.store.support.models.*;
import com.sellam.store.support.repositories.SupportAuditLogRepository;
import com.sellam.store.support.repositories.TicketMessageRepository;
import com.sellam.store.support.repositories.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private TicketMessageRepository ticketMessageRepository;
    @Mock private SupportAuditLogRepository auditLogRepository;
    @Mock private PersonRepository personRepository;

    @InjectMocks
    private SupportService supportService;

    private PersonEntity admin;
    private PersonEntity targetUser;

    @BeforeEach
    void setUp() {
        admin = new PersonEntity();
        admin.setId(UUID.randomUUID());
        admin.setName("Admin");

        targetUser = new PersonEntity();
        targetUser.setId(UUID.randomUUID());
        targetUser.setName("Client");
        targetUser.setLastPhoneChangeAt(java.time.LocalDateTime.now().minusDays(10));
        targetUser.setLastEmailChangeAt(java.time.LocalDateTime.now().minusDays(10));
    }

    @Test
    void testResetIdentityLimit_ClearsFields() {
        when(personRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(personRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));

        SupportDTO.ResetIdentityLimitRequest request = SupportDTO.ResetIdentityLimitRequest.builder()
                .reason("Client a perdu son téléphone, justificatif vérifié")
                .build();

        supportService.resetIdentityChangeLimit(admin.getId(), targetUser.getId(), request);

        // Vérifie que les dates ont été effacées
        assertNull(targetUser.getLastPhoneChangeAt());
        assertNull(targetUser.getLastEmailChangeAt());

        // Vérifie que l'utilisateur a été sauvegardé
        verify(personRepository).save(targetUser);

        // Vérifie qu'un log d'audit a été créé
        verify(auditLogRepository).save(argThat(log ->
                log.getAdmin().getId().equals(admin.getId()) &&
                log.getTargetUser().getId().equals(targetUser.getId()) &&
                log.getActionType() == SupportActionTypeEnum.RESET_IDENTITY_CHANGE_LIMIT &&
                log.getReason().contains("justificatif vérifié")
        ));
    }

    @Test
    void testResetIdentityLimit_WithTicket_CreatesSystemMessage() {
        TicketEntity ticket = TicketEntity.builder()
                .id(UUID.randomUUID())
                .author(targetUser)
                .subject("Changement téléphone")
                .status(TicketStatusEnum.IN_PROGRESS)
                .category(TicketCategoryEnum.ACCOUNT)
                .priority(TicketPriorityEnum.HIGH)
                .build();

        when(personRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(personRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        SupportDTO.ResetIdentityLimitRequest request = SupportDTO.ResetIdentityLimitRequest.builder()
                .reason("Déblocage suite au ticket")
                .ticketId(ticket.getId())
                .build();

        supportService.resetIdentityChangeLimit(admin.getId(), targetUser.getId(), request);

        // Vérifie qu'un message système a été créé dans le ticket
        verify(ticketMessageRepository).save(argThat(msg ->
                msg.isSystemMessage() &&
                msg.getTicket().getId().equals(ticket.getId()) &&
                msg.getMessage().contains("réinitialisé la limite")
        ));

        // Vérifie que le log d'audit référence le ticket
        verify(auditLogRepository).save(argThat(log ->
                log.getTicket() != null &&
                log.getTicket().getId().equals(ticket.getId())
        ));
    }

    @Test
    void testCreateTicket_CreatesTicketAndInitialMessage() {
        when(personRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));
        when(ticketRepository.save(any(TicketEntity.class))).thenAnswer(i -> {
            TicketEntity t = i.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        SupportDTO.CreateTicketRequest request = SupportDTO.CreateTicketRequest.builder()
                .subject("Mon compte est bloqué")
                .category(TicketCategoryEnum.ACCOUNT)
                .priority(TicketPriorityEnum.HIGH)
                .initialMessage("Je ne peux plus changer mon numéro")
                .build();

        SupportDTO.TicketSummaryResponse response = supportService.createTicket(targetUser.getId(), request);

        assertNotNull(response.getId());
        assertEquals("Mon compte est bloqué", response.getSubject());
        assertEquals(TicketStatusEnum.OPEN, response.getStatus());

        // Vérifie que le message initial a été créé
        verify(ticketMessageRepository).save(argThat(msg ->
                !msg.isSystemMessage() &&
                msg.getMessage().equals("Je ne peux plus changer mon numéro")
        ));
    }

    @Test
    void testGetTicketDetail_NonOwnerDenied() {
        PersonEntity otherUser = new PersonEntity();
        otherUser.setId(UUID.randomUUID());
        otherUser.setName("Intrus");

        TicketEntity ticket = TicketEntity.builder()
                .id(UUID.randomUUID())
                .author(targetUser)
                .build();

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThrows(IllegalStateException.class, () ->
                supportService.getTicketDetail(ticket.getId(), otherUser.getId(), false));
    }
}
