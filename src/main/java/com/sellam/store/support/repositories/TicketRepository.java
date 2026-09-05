package com.sellam.store.support.repositories;

import com.sellam.store.support.models.TicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<TicketEntity, UUID> {
    List<TicketEntity> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);
    List<TicketEntity> findAllByOrderByCreatedAtDesc();
}
