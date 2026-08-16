package com.sellam.store.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationResponse {
        private UUID id;
        private UUID shopId;
        private String type;
        private String title;
        private String message;
        private String status;
        private String recipientEmail;
        private LocalDateTime createdAt;
    }
}
