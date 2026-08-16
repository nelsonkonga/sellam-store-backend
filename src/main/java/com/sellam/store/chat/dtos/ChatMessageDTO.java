package com.sellam.store.chat.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDTO {
    private UUID id;
    private UUID shopId;
    private UUID senderId;
    private String senderName;
    private String content;
    private LocalDateTime createdAt;
    private String conversationId;
}
