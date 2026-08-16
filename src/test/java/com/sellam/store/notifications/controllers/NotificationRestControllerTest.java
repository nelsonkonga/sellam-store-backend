package com.sellam.store.notifications.controllers;

import com.sellam.store.common.security.ShopAccessGuard;
import com.sellam.store.notifications.dto.NotificationDTO;
import com.sellam.store.notifications.models.NotificationEntity;
import com.sellam.store.notifications.repositories.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationRestControllerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ShopAccessGuard shopAccessGuard;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private NotificationRestController notificationRestController;

    @Test
    void listNotifications_shouldReturnLatestNotificationsForShop() {
        UUID shopId = UUID.randomUUID();

        NotificationEntity notification = NotificationEntity.builder()
                .id(UUID.randomUUID())
                .shopId(shopId)
                .notificationType(NotificationEntity.NotificationType.BALANCE_REMINDER)
                .title("Rappel de bilan")
                .message("Il est temps de faire le bilan")
                .status(NotificationEntity.NotificationStatus.SENT)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findByShopIdOrderByCreatedAtDesc(shopId))
                .thenReturn(List.of(notification));

        List<NotificationDTO.NotificationResponse> result = notificationRestController.listNotifications(shopId, authentication);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Rappel de bilan");
        assertThat(result.get(0).getType()).isEqualTo(NotificationEntity.NotificationType.BALANCE_REMINDER.name());
        verify(shopAccessGuard).requireShopAccess(authentication, shopId);
    }
}
