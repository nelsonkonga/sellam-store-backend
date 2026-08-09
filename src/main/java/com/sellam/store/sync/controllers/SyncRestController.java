package com.sellam.store.sync.controllers;

import com.sellam.store.sync.dto.SyncDTO;
import com.sellam.store.sync.services.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sync")
public class SyncRestController
{

    private final SyncService syncService;

    public SyncRestController(SyncService syncService)
    {
        this.syncService = syncService;
    }

    @PostMapping
    public ResponseEntity<SyncDTO.SyncResponse> sync(@RequestBody SyncDTO.SyncRequest request)
    {
        return ResponseEntity.ok(syncService.processSync(request));
    }
}