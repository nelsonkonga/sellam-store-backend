package com.sellam.store.cash.controllers;

import com.sellam.store.cash.dto.CashDTO;
import com.sellam.store.cash.services.CashSessionService;
import com.sellam.store.common.security.AuthPrincipal;
import com.sellam.store.common.security.IShopAccessGuard;
import com.sellam.store.subscriptions.security.SubscriptionWriteGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cash")
public class CashSessionController {

    private final CashSessionService cashSessionService;
    private final IShopAccessGuard shopAccessGuard;
    private final SubscriptionWriteGuard subscriptionWriteGuard;

    // ──────────────── Register ────────────────

    @GetMapping("/registers/{shopId}")
    @ResponseStatus(HttpStatus.OK)
    public List<CashDTO.RegisterResponse> listRegisters(@PathVariable UUID shopId, Authentication auth) {
        shopAccessGuard.checkShopAccess(auth, shopId);
        return cashSessionService.listRegisters(shopId);
    }

    @PostMapping("/registers/{shopId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CashDTO.RegisterResponse createRegister(
            @PathVariable UUID shopId,
            @Valid @RequestBody CashDTO.CreateRegisterRequest request,
            Authentication auth) {
        shopAccessGuard.checkShopAccess(auth, shopId);
        return cashSessionService.createRegister(shopId, request);
    }

    // ──────────────── Session Lifecycle ────────────────

    @PostMapping("/sessions/open/{registerId}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public CashDTO.SessionSummary openSession(
            @PathVariable UUID registerId,
            @Valid @RequestBody CashDTO.OpenSessionRequest request,
            Authentication auth) {
        AuthPrincipal principal = (AuthPrincipal) auth.getPrincipal();
        UUID registerShopId = cashSessionService.getRegisterShopId(registerId);
        shopAccessGuard.checkShopAccess(auth, registerShopId);
        subscriptionWriteGuard.assertWritable(registerShopId);
        return cashSessionService.openSession(registerId, principal.getId(), request);
    }

    @PostMapping("/sessions/{sessionId}/close")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public CashDTO.SessionSummary closeSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody CashDTO.CloseSessionRequest request,
            Authentication auth) {
        AuthPrincipal principal = (AuthPrincipal) auth.getPrincipal();
        UUID closeShopId = cashSessionService.getSessionShopId(sessionId);
        shopAccessGuard.checkShopAccess(auth, closeShopId);
        subscriptionWriteGuard.assertWritable(closeShopId);
        return cashSessionService.closeSession(sessionId, principal.getId(), request);
    }

    @PostMapping("/sessions/{sessionId}/regularize")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public CashDTO.SessionSummary regularizeInitialCash(
            @PathVariable UUID sessionId,
            @Valid @RequestBody CashDTO.RegularizeInitialCashRequest request,
            Authentication auth) {
        AuthPrincipal principal = (AuthPrincipal) auth.getPrincipal();
        UUID regularizeShopId = cashSessionService.getSessionShopId(sessionId);
        shopAccessGuard.checkShopAccess(auth, regularizeShopId);
        subscriptionWriteGuard.assertWritable(regularizeShopId);
        return cashSessionService.regularizeInitialCash(sessionId, principal.getId(), request);
    }

    @PostMapping("/sessions/handover/resolve")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public CashDTO.SessionSummary resolveHandover(
            @RequestParam UUID pendingSessionId,
            @RequestParam UUID currentSessionId,
            @Valid @RequestBody CashDTO.HandoverCountRequest request,
            Authentication auth) {
        AuthPrincipal principal = (AuthPrincipal) auth.getPrincipal();
        UUID pendingShopId = cashSessionService.getSessionShopId(pendingSessionId);
        UUID currentShopId = cashSessionService.getSessionShopId(currentSessionId);
        shopAccessGuard.checkShopAccess(auth, pendingShopId);
        shopAccessGuard.checkShopAccess(auth, currentShopId);
        subscriptionWriteGuard.assertWritable(pendingShopId);
        subscriptionWriteGuard.assertWritable(currentShopId);
        return cashSessionService.resolveHandover(pendingSessionId, currentSessionId, principal.getId(), request);
    }

    // ──────────────── Movements ────────────────

    @PostMapping("/sessions/{sessionId}/movements")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public CashDTO.MovementResponse addMovement(
            @PathVariable UUID sessionId,
            @Valid @RequestBody CashDTO.CreateMovementRequest request,
            Authentication auth) {
        AuthPrincipal principal = (AuthPrincipal) auth.getPrincipal();
        UUID movementShopId = cashSessionService.getSessionShopId(sessionId);
        shopAccessGuard.checkShopAccess(auth, movementShopId);
        subscriptionWriteGuard.assertWritable(movementShopId);
        return cashSessionService.addMovement(sessionId, principal.getId(), request);
    }

    // ──────────────── Queries ────────────────

    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public CashDTO.SessionDetailResponse getSessionDetail(@PathVariable UUID sessionId, Authentication auth) {
        shopAccessGuard.checkShopAccess(auth, cashSessionService.getSessionShopId(sessionId));
        return cashSessionService.getSessionDetail(sessionId);
    }

    @GetMapping("/sessions/shop/{shopId}")
    @ResponseStatus(HttpStatus.OK)
    public List<CashDTO.SessionSummary> listSessions(@PathVariable UUID shopId, Authentication auth) {
        shopAccessGuard.checkShopAccess(auth, shopId);
        return cashSessionService.listSessions(shopId);
    }

    @GetMapping("/status/{shopId}")
    @ResponseStatus(HttpStatus.OK)
    public CashDTO.CashStatusResponse getCashStatus(@PathVariable UUID shopId, Authentication auth) {
        shopAccessGuard.checkShopAccess(auth, shopId);
        return cashSessionService.getCashStatus(shopId);
    }
}
