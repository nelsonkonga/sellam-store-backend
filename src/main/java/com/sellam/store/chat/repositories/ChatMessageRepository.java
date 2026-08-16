package com.sellam.store.chat.repositories;

import com.sellam.store.chat.models.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);
    List<ChatMessage> findByShopIdOrderByCreatedAtDesc(UUID shopId);
}
