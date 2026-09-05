package com.sellam.store.support.repositories;

import com.sellam.store.support.models.TicketMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessageEntity, UUID> {
    List<TicketMessageEntity> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
