package com.sellam.store.notifications.services;

import com.sellam.store.notifications.models.PushSubscriptionEntity;
import com.sellam.store.notifications.repositories.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PushSubscriptionService {

    private final PushSubscriptionRepository pushSubscriptionRepository;

    public PushSubscriptionEntity saveSubscription(PushSubscriptionEntity subscription) {
        if (subscription == null) {
            throw new IllegalArgumentException("Subscription cannot be null");
        }
        
        return pushSubscriptionRepository.findByEndpoint(subscription.getEndpoint())
                .map(existing -> {
                    existing.setShopId(subscription.getShopId());
                    existing.setP256dh(subscription.getP256dh());
                    existing.setAuth(subscription.getAuth());
                    return pushSubscriptionRepository.save(existing);
                })
                .orElseGet(() -> pushSubscriptionRepository.save(subscription));
    }

    public List<PushSubscriptionEntity> getSubscriptionsByShop(UUID shopId) {
        return pushSubscriptionRepository.findByShopId(shopId);
    }
}
