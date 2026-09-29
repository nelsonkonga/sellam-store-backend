package com.sellam.store.sync.controllers;

import com.sellam.store.common.security.AuthPrincipal;
import com.sellam.store.common.security.IShopAccessGuard;
import com.sellam.store.subscriptions.security.SubscriptionWriteGuard;
import com.sellam.store.sync.dto.SyncDTO;
import com.sellam.store.sync.services.SyncService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Chaque PendingAction transporte son propre shopId (le flux offline peut
 * accumuler des actions sur plusieurs boutiques avant reconnexion). On vérifie
 * donc l'accès individuellement par action, plutôt qu'une seule fois pour
 * toute la requête.
 */
@RestController
@RequestMapping("/api/sync")
public class SyncRestController
{

    private final SyncService syncService;
    private final IShopAccessGuard shopAccessGuard;
    private final SubscriptionWriteGuard subscriptionWriteGuard;

    public SyncRestController(SyncService syncService, IShopAccessGuard shopAccessGuard, SubscriptionWriteGuard subscriptionWriteGuard)
    {
        this.syncService = syncService;
        this.shopAccessGuard = shopAccessGuard;
        this.subscriptionWriteGuard = subscriptionWriteGuard;
    }

    @PostMapping
    public ResponseEntity<SyncDTO.SyncResponse> sync(
            @RequestBody SyncDTO.SyncRequest request,
            Authentication authentication
    )
    {
        AuthPrincipal principal = shopAccessGuard.requirePrincipal(authentication);

        if (request.getActions() == null || request.getActions().isEmpty())
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune action à synchroniser");
        }

        for (SyncDTO.PendingAction action : request.getActions())
        {
            if (action.getShopId() == null)
            {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une action de synchronisation ne référence aucune boutique");
            }

            UUID actionShopId;
            try
            {
                actionShopId = UUID.fromString(action.getShopId());
            }
            catch (IllegalArgumentException e)
            {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une action de synchronisation référence un identifiant de boutique invalide");
            }

            shopAccessGuard.checkShopAccess(principal, actionShopId);
            subscriptionWriteGuard.assertWritable(actionShopId);
        }

        return ResponseEntity.ok(syncService.processSync(request, principal));
    }
}