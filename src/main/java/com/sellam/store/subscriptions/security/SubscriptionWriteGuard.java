package com.sellam.store.subscriptions.security;

import com.sellam.store.subscriptions.models.SubscriptionStatusEnum;
import com.sellam.store.subscriptions.repositories.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionWriteGuard
{
    private final SubscriptionRepository subscriptionRepository;

    public boolean isExpired(UUID shopId)
    {
        if (shopId == null)
        {
            return false;
        }
        return subscriptionRepository.findByShopId(shopId)
                .map(subscription -> subscription.getStatus() == SubscriptionStatusEnum.EXPIRED)
                .orElse(false);
    }

    public void assertWritable(UUID shopId)
    {
        if (isExpired(shopId))
        {
            throw new SubscriptionExpiredException();
        }
    }
}
