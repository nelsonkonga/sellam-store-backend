package com.sellam.store.subscriptions.schedulers;

import com.sellam.store.common.email.services.EmailService;
import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.models.ShopMembershipEntity;
import com.sellam.store.identity.repositories.ShopMembershipRepository;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.subscriptions.models.SubscriptionEntity;
import com.sellam.store.subscriptions.models.SubscriptionStatusEnum;
import com.sellam.store.subscriptions.repositories.SubscriptionRepository;
import com.sellam.store.subscriptions.services.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionSchedulerTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private ShopMembershipRepository shopMembershipRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private SubscriptionScheduler subscriptionScheduler;

    private ShopEntity shop;

    @BeforeEach
    void setUp() {
        shop = ShopEntity.builder()
                .id(UUID.randomUUID())
                .name("Boutique Test")
                .build();
    }

    @Test
    void testRecalculateAndWarnDoesNotSaveWhenStatusUnchanged() {
        SubscriptionEntity subscription = SubscriptionEntity.builder()
                .id(UUID.randomUUID())
                .shop(shop)
                .status(SubscriptionStatusEnum.ACTIVE)
                .currentPeriodEndsAt(LocalDateTime.now().plusDays(10))
                .build();

        when(subscriptionRepository.findByStatusIn(anyList()))
                .thenReturn(List.of(subscription));

        // When recalculate runs, status remains ACTIVE
        doNothing().when(subscriptionService).recalculate(subscription);

        subscriptionScheduler.recalculateAndWarn();

        // Verify save is NEVER called because before == after
        verify(subscriptionRepository, never()).save(any(SubscriptionEntity.class));
        verify(emailService, never()).sendSubscriptionTrialEndingEmail(any(), any());
    }

    @Test
    void testRecalculateAndWarnSavesAndNotifiesWhenStatusChanges() {
        SubscriptionEntity subscription = SubscriptionEntity.builder()
                .id(UUID.randomUUID())
                .shop(shop)
                .status(SubscriptionStatusEnum.TRIAL)
                .trialEndsAt(LocalDateTime.now().plusDays(2))
                .build();

        when(subscriptionRepository.findByStatusIn(anyList()))
                .thenReturn(List.of(subscription));

        // Simulate recalculate changing status to TRIAL_ENDING
        doAnswer(invocation -> {
            SubscriptionEntity sub = invocation.getArgument(0);
            sub.setStatus(SubscriptionStatusEnum.TRIAL_ENDING);
            return null;
        }).when(subscriptionService).recalculate(subscription);

        PersonEntity person = PersonEntity.builder()
                .id(UUID.randomUUID())
                .email("gerant@test.com")
                .build();
        ShopMembershipEntity membership = ShopMembershipEntity.builder()
                .shop(shop)
                .person(person)
                .active(true)
                .build();

        when(shopMembershipRepository.findByShopId(shop.getId()))
                .thenReturn(List.of(membership));

        subscriptionScheduler.recalculateAndWarn();

        // Verify save IS called because before != after
        verify(subscriptionRepository, times(1)).save(subscription);
        verify(emailService, times(1)).sendSubscriptionTrialEndingEmail(eq("gerant@test.com"), eq("Boutique Test"));
    }
}
