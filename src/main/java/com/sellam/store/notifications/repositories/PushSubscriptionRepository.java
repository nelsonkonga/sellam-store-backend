package com.sellam.store.notifications.repositories;

import com.sellam.store.notifications.models.PushSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscriptionEntity, UUID> {
    List<PushSubscriptionEntity> findByShopId(UUID shopId);
    boolean existsByEndpoint(String endpoint);
    java.util.Optional<PushSubscriptionEntity> findByEndpoint(String endpoint);
}
