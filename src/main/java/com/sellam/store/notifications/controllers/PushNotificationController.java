package com.sellam.store.notifications.controllers;
import org.springframework.context.annotation.Profile;

import com.sellam.store.common.security.IShopAccessGuard;
import com.sellam.store.notifications.models.PushSubscriptionEntity;
import com.sellam.store.notifications.services.PushNotificationService;
import com.sellam.store.notifications.services.PushSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class PushNotificationController {

    private final PushSubscriptionService pushSubscriptionService;
    private final PushNotificationService pushNotificationService;
    private final IShopAccessGuard shopAccessGuard;

    @Value("${app.vapid.public-key}")
    private String vapidPublicKey;

    @GetMapping("/push/vapid-key")
    public Map<String, String> getVapidPublicKey() {
        return Map.of("publicKey", vapidPublicKey);
    }

    @PostMapping("/push/subscribe")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> subscribe(
            @RequestParam UUID shopId,
            @RequestBody PushSubscriptionPayload payload,
            Authentication authentication
    ) {
        shopAccessGuard.requireShopAccess(authentication, shopId);

        log.info("Push subscribe request: shopId={}, endpoint={}..., p256dh={}, auth={}",
                shopId,
                payload.endpoint() != null ? payload.endpoint().substring(0, Math.min(60, payload.endpoint().length())) : "null",
                payload.p256dh() != null ? payload.p256dh().substring(0, Math.min(20, payload.p256dh().length())) + "..." : "null",
                payload.auth() != null ? payload.auth().substring(0, Math.min(10, payload.auth().length())) + "..." : "null"
        );

        if (payload.p256dh() == null || payload.p256dh().isBlank() || payload.auth() == null || payload.auth().isBlank()) {
            log.error("Push subscribe REJECTED: p256dh or auth is null/empty for shopId={}", shopId);
            return Map.of("status", "error", "message", "Missing p256dh or auth keys");
        }

        PushSubscriptionEntity entity = PushSubscriptionEntity.builder()
                .shopId(shopId)
                .endpoint(payload.endpoint())
                .p256dh(payload.p256dh())
                .auth(payload.auth())
                .build();

        pushSubscriptionService.saveSubscription(entity);
        log.info("Push subscription saved successfully for shopId={}", shopId);
        return Map.of("status", "subscribed");
    }

    public record PushSubscriptionPayload(
            String endpoint,
            String p256dh,
            String auth
    ) {}

    @PostMapping("/push/test")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> testNotification(
            @RequestParam UUID shopId,
            @RequestParam(defaultValue = "Test de Notification") String title,
            @RequestParam(defaultValue = "Ceci est un test de la configuration Web Push Sellam.") String message,
            Authentication authentication
    ) {
        shopAccessGuard.requireShopAccess(authentication, shopId);
        log.info("Push test requested for shopId={}", shopId);
        
        pushNotificationService.sendToShop(shopId, title, message);
        return Map.of("status", "sent");
    }
}

