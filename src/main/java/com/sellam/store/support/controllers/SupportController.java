package com.sellam.store.support.controllers;

import com.sellam.store.common.security.AuthPrincipal;
import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.models.SystemRoleEnum;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.support.dto.SupportDTO;
import com.sellam.store.support.services.SupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/support")
public class SupportController {

    private final SupportService supportService;
    private final PersonRepository personRepository;

    // --- User Endpoints ---

    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public SupportDTO.TicketSummaryResponse createTicket(
            @Valid @RequestBody SupportDTO.CreateTicketRequest request,
            Authentication auth) {
        AuthPrincipal principal = (AuthPrincipal) auth.getPrincipal();
        return supportService.createTicket(principal.getId(), request);
    }

    @GetMapping("/tickets/my")
    @ResponseStatus(HttpStatus.OK)
    public List<SupportDTO.TicketSummaryResponse> listMyTickets(Authentication auth) {
        AuthPrincipal principal = (AuthPrincipal) auth.getPrincipal();
        return supportService.listUserTickets(principal.getId());
    }

    @GetMapping("/tickets/{ticketId}")
    @ResponseStatus(HttpStatus.OK)
    public SupportDTO.TicketDetailResponse getTicket(
            @PathVariable UUID ticketId,
            Authentication auth) {
        AuthPrincipal principal = (AuthPrincipal) auth.getPrincipal();
        boolean isAdmin = isPlatformAdmin(principal.getId());
        return supportService.getTicketDetail(ticketId, principal.getId(), isAdmin);
    }

    @PostMapping("/tickets/{ticketId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public SupportDTO.TicketMessageResponse addMessage(
            @PathVariable UUID ticketId,
            @Valid @RequestBody SupportDTO.AddMessageRequest request,
            Authentication auth) {
        AuthPrincipal principal = (AuthPrincipal) auth.getPrincipal();
        boolean isAdmin = isPlatformAdmin(principal.getId());
        return supportService.addMessage(ticketId, principal.getId(), request, isAdmin);
    }

    // --- Admin Endpoints ---

    @GetMapping("/admin/tickets")
    @ResponseStatus(HttpStatus.OK)
    public List<SupportDTO.TicketSummaryResponse> listAllTickets(Authentication auth) {
        requireAdmin(auth);
        return supportService.listAllTickets();
    }

    @PutMapping("/admin/tickets/{ticketId}/status")
    @ResponseStatus(HttpStatus.OK)
    public SupportDTO.TicketSummaryResponse updateTicketStatus(
            @PathVariable UUID ticketId,
            @Valid @RequestBody SupportDTO.UpdateTicketStatusRequest request,
            Authentication auth) {
        requireAdmin(auth);
        return supportService.updateTicketStatus(ticketId, request);
    }

    @PostMapping("/admin/users/{userId}/reset-identity-limits")
    @ResponseStatus(HttpStatus.OK)
    public void resetIdentityLimits(
            @PathVariable UUID userId,
            @Valid @RequestBody SupportDTO.ResetIdentityLimitRequest request,
            Authentication auth) {
        requireAdmin(auth);
        AuthPrincipal principal = (AuthPrincipal) auth.getPrincipal();
        supportService.resetIdentityChangeLimit(principal.getId(), userId, request);
    }

    // --- Helpers ---

    private boolean isPlatformAdmin(UUID personId) {
        return personRepository.findById(personId)
                .map(p -> p.getSystemRole() == SystemRoleEnum.PLATFORM_ADMIN)
                .orElse(false);
    }

    private void requireAdmin(Authentication auth) {
        AuthPrincipal principal = (AuthPrincipal) auth.getPrincipal();
        if (!isPlatformAdmin(principal.getId())) {
            throw new IllegalStateException("Accès refusé. Réservé aux administrateurs de la plateforme.");
        }
    }
}
