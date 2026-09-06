package com.sellam.store.dashboard.controllers;

import com.sellam.store.common.security.IShopAccessGuard;
import com.sellam.store.dashboard.dto.DashboardDTO;
import com.sellam.store.dashboard.services.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardRestController {

    private final DashboardService dashboardService;
    private final IShopAccessGuard shopAccessGuard;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardDTO.DashboardResponse> getDashboard(
            @RequestParam UUID shopId,
            Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
        return ResponseEntity.ok(dashboardService.getDashboardData(shopId));
    }
}
