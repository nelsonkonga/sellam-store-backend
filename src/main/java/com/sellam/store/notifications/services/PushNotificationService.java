package com.sellam.store.notifications.services;

import com.sellam.store.notifications.models.PushSubscriptionEntity;
import com.sellam.store.notifications.repositories.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.jose4j.lang.JoseException;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!dev")
public class PushNotificationService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final PushService pushService;

    public void sendToShop(UUID shopId, String title, String body) {
        List<PushSubscriptionEntity> subscriptions = pushSubscriptionRepository.findByShopId(shopId);
        log.info("Sending push to shopId={}: found {} subscription(s)", shopId, subscriptions.size());

        if (subscriptions.isEmpty()) {
            log.warn("No push subscriptions found for shopId={}. The notification won't be delivered.", shopId);
            return;
        }

        String payload = "{\"title\":\"" + escapeJson(title) + "\",\"body\":\"" + escapeJson(body)
                + "\",\"icon\":\"/icon-192.png\",\"badge\":\"/icon-192.png\"}";
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

        for (PushSubscriptionEntity subscription : subscriptions) {
            try {
                log.info("Sending push to endpoint: {}...", subscription.getEndpoint().substring(0, Math.min(80, subscription.getEndpoint().length())));
                log.debug("  p256dh={}, auth={}", subscription.getP256dh(), subscription.getAuth());
                Notification notification = new Notification(
                        subscription.getEndpoint(),
                        subscription.getP256dh(),
                        subscription.getAuth(),
                        payloadBytes,
                        3600
                );
                pushService.send(notification);
                log.info("Push notification sent successfully to endpoint.");
            } catch (GeneralSecurityException | IOException | JoseException | ExecutionException e) {
                log.error("Failed to send push notification to endpoint {}: {}", subscription.getEndpoint(), e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Push notification sending interrupted for endpoint {}: {}", subscription.getEndpoint(), e.getMessage());
                break;
            }
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
