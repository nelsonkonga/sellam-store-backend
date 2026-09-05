package com.sellam.store.support.services;

import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.support.dto.SupportDTO;
import com.sellam.store.support.models.*;
import com.sellam.store.support.repositories.SupportAuditLogRepository;
import com.sellam.store.support.repositories.TicketMessageRepository;
import com.sellam.store.support.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportService {

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final SupportAuditLogRepository auditLogRepository;
    private final PersonRepository personRepository;

    // --- User Actions ---

    @Transactional
    public SupportDTO.TicketSummaryResponse createTicket(UUID authorId, SupportDTO.CreateTicketRequest request) {
        PersonEntity author = findPerson(authorId);

        TicketEntity ticket = TicketEntity.builder()
                .author(author)
                .subject(request.getSubject())
                .category(request.getCategory())
                .priority(request.getPriority())
                .status(TicketStatusEnum.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        ticket = ticketRepository.save(ticket);

        TicketMessageEntity message = TicketMessageEntity.builder()
                .ticket(ticket)
                .sender(author)
                .message(request.getInitialMessage())
                .isSystemMessage(false)
                .createdAt(LocalDateTime.now())
                .build();
        ticketMessageRepository.save(message);

        return toTicketSummary(ticket);
    }

    @Transactional
    public SupportDTO.TicketMessageResponse addMessage(UUID ticketId, UUID senderId, SupportDTO.AddMessageRequest request, boolean isAdmin) {
        TicketEntity ticket = findTicket(ticketId);
        PersonEntity sender = findPerson(senderId);

        // Update status automatically
        if (isAdmin) {
            ticket.setStatus(TicketStatusEnum.WAITING_FOR_USER);
        } else {
            ticket.setStatus(TicketStatusEnum.IN_PROGRESS);
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        TicketMessageEntity message = TicketMessageEntity.builder()
                .ticket(ticket)
                .sender(sender)
                .message(request.getMessage())
                .isSystemMessage(false)
                .createdAt(LocalDateTime.now())
                .build();
        message = ticketMessageRepository.save(message);

        return toMessageResponse(message);
    }

    public List<SupportDTO.TicketSummaryResponse> listUserTickets(UUID authorId) {
        return ticketRepository.findByAuthorIdOrderByCreatedAtDesc(authorId)
                .stream().map(this::toTicketSummary).collect(Collectors.toList());
    }

    // --- Admin Actions ---

    public List<SupportDTO.TicketSummaryResponse> listAllTickets() {
        return ticketRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toTicketSummary).collect(Collectors.toList());
    }

    @Transactional
    public SupportDTO.TicketSummaryResponse updateTicketStatus(UUID ticketId, SupportDTO.UpdateTicketStatusRequest request) {
        TicketEntity ticket = findTicket(ticketId);
        ticket.setStatus(request.getStatus());
        ticket.setUpdatedAt(LocalDateTime.now());
        
        if (request.getStatus() == TicketStatusEnum.RESOLVED || request.getStatus() == TicketStatusEnum.CLOSED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        
        return toTicketSummary(ticketRepository.save(ticket));
    }

    @Transactional
    public void resetIdentityChangeLimit(UUID adminId, UUID targetUserId, SupportDTO.ResetIdentityLimitRequest request) {
        PersonEntity admin = findPerson(adminId);
        PersonEntity targetUser = findPerson(targetUserId);

        // 1. Reset the limit
        targetUser.setLastEmailChangeAt(null);
        targetUser.setLastPhoneChangeAt(null);
        personRepository.save(targetUser);

        // 2. Audit Log
        TicketEntity relatedTicket = null;
        if (request.getTicketId() != null) {
            relatedTicket = findTicket(request.getTicketId());
        }

        SupportAuditLogEntity auditLog = SupportAuditLogEntity.builder()
                .admin(admin)
                .targetUser(targetUser)
                .actionType(SupportActionTypeEnum.RESET_IDENTITY_CHANGE_LIMIT)
                .reason(request.getReason())
                .ticket(relatedTicket)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        log.info("L'administrateur {} a réinitialisé la limite d'identité pour {}. Motif : {}", 
                admin.getName(), targetUser.getName(), request.getReason());

        // 3. System message in ticket if provided
        if (relatedTicket != null) {
            TicketMessageEntity systemMessage = TicketMessageEntity.builder()
                    .ticket(relatedTicket)
                    .sender(admin) // The admin triggered it
                    .message("Action Système : L'administrateur a réinitialisé la limite de changement de téléphone/email.")
                    .isSystemMessage(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            ticketMessageRepository.save(systemMessage);
        }
    }

    // --- Queries ---

    public SupportDTO.TicketDetailResponse getTicketDetail(UUID ticketId, UUID userId, boolean isAdmin) {
        TicketEntity ticket = findTicket(ticketId);
        
        // Security check: only author or admin can view
        if (!isAdmin && !ticket.getAuthor().getId().equals(userId)) {
            throw new IllegalStateException("Vous n'êtes pas autorisé à voir ce ticket");
        }

        List<SupportDTO.TicketMessageResponse> messages = ticketMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream().map(this::toMessageResponse).collect(Collectors.toList());

        return SupportDTO.TicketDetailResponse.builder()
                .ticket(toTicketSummary(ticket))
                .messages(messages)
                .build();
    }

    // --- Helpers ---

    private PersonEntity findPerson(UUID id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    private TicketEntity findTicket(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable"));
    }

    private SupportDTO.TicketSummaryResponse toTicketSummary(TicketEntity ticket) {
        return SupportDTO.TicketSummaryResponse.builder()
                .id(ticket.getId())
                .authorName(ticket.getAuthor().getName())
                .subject(ticket.getSubject())
                .category(ticket.getCategory())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    private SupportDTO.TicketMessageResponse toMessageResponse(TicketMessageEntity message) {
        return SupportDTO.TicketMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSender() != null ? message.getSender().getId() : null)
                .senderName(message.getSender() != null ? message.getSender().getName() : "Système")
                .message(message.getMessage())
                .isSystemMessage(message.isSystemMessage())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
