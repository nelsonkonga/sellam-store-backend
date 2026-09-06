package com.sellam.store.subscriptions.repositories;

import com.sellam.store.subscriptions.models.SubscriptionEntity;
import com.sellam.store.subscriptions.models.SubscriptionStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID>
{
    Optional<SubscriptionEntity> findByShopId(UUID shopId);

    List<SubscriptionEntity> findByStatusAndTrialEndsAtBefore(SubscriptionStatusEnum status, LocalDateTime before);

    List<SubscriptionEntity> findByStatusAndTrialEndsAtBetween(
            SubscriptionStatusEnum status, LocalDateTime start, LocalDateTime end);

    List<SubscriptionEntity> findByStatusAndCurrentPeriodEndsAtBefore(
            SubscriptionStatusEnum status, LocalDateTime before);

    List<SubscriptionEntity> findByStatusAndGraceUntilBefore(
            SubscriptionStatusEnum status, LocalDateTime before);

    List<SubscriptionEntity> findByStatusIn(List<SubscriptionStatusEnum> statuses);
}
