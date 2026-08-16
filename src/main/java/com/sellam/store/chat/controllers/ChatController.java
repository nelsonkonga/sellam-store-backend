package com.sellam.store.chat.controllers;

import com.sellam.store.chat.dtos.ChatMessageDTO;
import com.sellam.store.chat.models.ChatMessage;
import com.sellam.store.chat.repositories.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;

    /**
     * WebSocket endpoint for sending messages
     * Client sends to: /app/chat/{conversationId}
     * Message is broadcast to: /topic/chat/{conversationId}
     */
    @MessageMapping("/chat/{conversationId}")
    @SendTo("/topic/chat/{conversationId}")
    public ChatMessageDTO sendMessage(
            @DestinationVariable String conversationId,
            ChatMessageDTO messageDTO) {

        // Save message to database
        ChatMessage chatMessage = ChatMessage.builder()
                .shopId(messageDTO.getShopId())
                .senderId(messageDTO.getSenderId())
                .senderName(messageDTO.getSenderName())
                .content(messageDTO.getContent())
                .conversationId(conversationId)
                .createdAt(LocalDateTime.now())
                .build();

        ChatMessage saved = chatMessageRepository.save(chatMessage);

        // Return to all subscribers of this conversation
        return ChatMessageDTO.builder()
                .id(saved.getId())
                .shopId(saved.getShopId())
                .senderId(saved.getSenderId())
                .senderName(saved.getSenderName())
                .content(saved.getContent())
                .conversationId(saved.getConversationId())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    /**
     * REST endpoint to fetch chat history
     */
    @GetMapping("/api/chat/history/{conversationId}")
    public List<ChatMessageDTO> getChatHistory(@PathVariable String conversationId) {
        return chatMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(msg -> ChatMessageDTO.builder()
                        .id(msg.getId())
                        .shopId(msg.getShopId())
                        .senderId(msg.getSenderId())
                        .senderName(msg.getSenderName())
                        .content(msg.getContent())
                        .conversationId(msg.getConversationId())
                        .createdAt(msg.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * REST endpoint to get recent messages for a shop
     */
    @GetMapping("/api/chat/shop/{shopId}")
    public List<ChatMessageDTO> getShopMessages(@PathVariable UUID shopId) {
        return chatMessageRepository
                .findByShopIdOrderByCreatedAtDesc(shopId)
                .stream()
                .map(msg -> ChatMessageDTO.builder()
                        .id(msg.getId())
                        .shopId(msg.getShopId())
                        .senderId(msg.getSenderId())
                        .senderName(msg.getSenderName())
                        .content(msg.getContent())
                        .conversationId(msg.getConversationId())
                        .createdAt(msg.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
