package com.sellam.store.support.repositories;

import com.sellam.store.support.models.TicketAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachmentEntity, UUID> {

    List<TicketAttachmentEntity> findByMessageId(UUID messageId);
}
