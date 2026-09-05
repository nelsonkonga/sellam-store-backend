package com.sellam.store.notifications.controllers;

import com.sellam.store.common.security.IShopAccessGuard;
import com.sellam.store.notifications.dto.NotificationDTO;
import com.sellam.store.notifications.models.NotificationEntity;
import com.sellam.store.notifications.repositories.NotificationRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationRestController {

    private final NotificationRepository notificationRepository;
    private final IShopAccessGuard shopAccessGuard;

    @PreAuthorize("@sec.can(authentication, 'VIEW_DAILY_BALANCE')")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<NotificationDTO.NotificationResponse> listNotifications(
            @RequestParam UUID shopId,
            Authentication authentication
    ) {
        shopAccessGuard.requireShopAccess(authentication, shopId);

        return notificationRepository.findByShopIdOrderByCreatedAtDesc(shopId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private NotificationDTO.NotificationResponse toDto(NotificationEntity entity) {
        return NotificationDTO.NotificationResponse.builder()
                .id(entity.getId())
                .shopId(entity.getShopId())
                .type(entity.getNotificationType() == null ? null : entity.getNotificationType().name())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .status(entity.getStatus() == null ? null : entity.getStatus().name())
                .recipientEmail(entity.getRecipientEmail())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
