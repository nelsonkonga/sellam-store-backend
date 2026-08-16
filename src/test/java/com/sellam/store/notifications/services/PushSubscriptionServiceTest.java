package com.sellam.store.notifications.services;

import com.sellam.store.notifications.models.PushSubscriptionEntity;
import com.sellam.store.notifications.repositories.PushSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PushSubscriptionServiceTest {

    @Mock
    private PushSubscriptionRepository pushSubscriptionRepository;

    @InjectMocks
    private PushSubscriptionService pushSubscriptionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void saveSubscription_shouldPersistCanonicalSubscriptionData() {
        UUID shopId = UUID.randomUUID();
        PushSubscriptionEntity subscription = PushSubscriptionEntity.builder()
                .shopId(shopId)
                .endpoint("https://fcm.googleapis.com/fcm/send/test-endpoint")
                .p256dh("p256dh-key")
                .auth("auth-secret")
                .build();

        when(pushSubscriptionRepository.save(any(PushSubscriptionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PushSubscriptionEntity saved = pushSubscriptionService.saveSubscription(subscription);

        assertThat(saved.getEndpoint()).isEqualTo("https://fcm.googleapis.com/fcm/send/test-endpoint");
        assertThat(saved.getP256dh()).isEqualTo("p256dh-key");
        assertThat(saved.getAuth()).isEqualTo("auth-secret");
        verify(pushSubscriptionRepository).save(subscription);
    }
}
