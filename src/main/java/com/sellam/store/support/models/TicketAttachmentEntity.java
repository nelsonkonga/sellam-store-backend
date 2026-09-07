package com.sellam.store.support.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pièce jointe (image) attachée soit directement à un ticket (au moment de sa
 * création, via le premier message), soit à un message ultérieur de la
 * discussion. Toujours rattachée à un TicketMessageEntity : la création d'un
 * ticket génère elle-même un premier message système/utilisateur auquel la
 * pièce jointe s'accroche, pour garder un seul modèle de rattachement.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "support_ticket_attachments")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class TicketAttachmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "message_id", nullable = false, updatable = false)
    private TicketMessageEntity message;

    @Column(nullable = false)
    private String fileUrl;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long fileSizeBytes;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
