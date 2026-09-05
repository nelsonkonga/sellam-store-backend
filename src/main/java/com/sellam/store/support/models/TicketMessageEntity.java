package com.sellam.store.support.models;

import com.sellam.store.identity.models.PersonEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="support_ticket_messages")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class TicketMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private TicketEntity ticket;

    @ManyToOne
    @JoinColumn(name = "sender_id", updatable = false)
    private PersonEntity sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private boolean isSystemMessage = false;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
