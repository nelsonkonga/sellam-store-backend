package com.sellam.store.chat.controllers;

import com.sellam.store.chat.dtos.ChatMessageDTO;
import com.sellam.store.chat.models.ChatMessage;
import com.sellam.store.chat.repositories.ChatMessageRepository;
import com.sellam.store.common.security.AuthPrincipal;
import com.sellam.store.common.security.IShopAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final IShopAccessGuard shopAccessGuard;

    public ChatController(ChatMessageRepository chatMessageRepository, IShopAccessGuard shopAccessGuard) {
        this.chatMessageRepository = chatMessageRepository;
        this.shopAccessGuard = shopAccessGuard;
    }

    /**
     * WebSocket endpoint for sending messages
     * Client sends to: /app/chat/{conversationId}
     * Message is broadcast to: /topic/chat/{conversationId}
     */
    @MessageMapping("/chat/{conversationId}")
    @SendTo("/topic/chat/{conversationId}")
    public ChatMessageDTO sendMessage(
            @DestinationVariable String conversationId,
                        ChatMessageDTO messageDTO,
                        org.springframework.security.core.Authentication authentication) {

                AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
                if (!principal.getId().equals(messageDTO.getSenderId())) {
                        throw new org.springframework.web.server.ResponseStatusException(
                                        org.springframework.http.HttpStatus.FORBIDDEN, "Expéditeur invalide");
                }
                shopAccessGuard.checkShopAccess(authentication, messageDTO.getShopId());

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
    @PreAuthorize("isAuthenticated()")
    public List<ChatMessageDTO> getChatHistory(@PathVariable String conversationId, Authentication authentication) {
        // Verify user has access to the shop associated with this conversation
        List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        if (!messages.isEmpty()) {
            shopAccessGuard.checkShopAccess(authentication, messages.get(0).getShopId());
        }
        return messages
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
    @PreAuthorize("isAuthenticated()")
    public List<ChatMessageDTO> getShopMessages(@PathVariable UUID shopId, Authentication authentication) {
        shopAccessGuard.checkShopAccess(authentication, shopId);
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
