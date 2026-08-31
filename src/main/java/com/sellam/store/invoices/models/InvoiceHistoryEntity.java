package com.sellam.store.invoices.models;

import com.sellam.store.invoices.models.InvoiceEntity;
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
@Table(name = "invoice_history")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class InvoiceHistoryEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceEntity invoice;

    @Enumerated(EnumType.STRING)
    private InvoiceHistoryActionType actionType;

    private String actorName; // Nom de l'utilisateur qui a effectué l'action
    private String actorId; // ID de l'utilisateur pour référence

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String details; // Détails de la modification (JSON ou texte avec avant/après)
}
