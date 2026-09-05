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
@Table(name="support_audit_logs")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class SupportAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "admin_id", nullable = false, updatable = false)
    private PersonEntity admin;

    @ManyToOne(optional = false)
    @JoinColumn(name = "target_user_id", nullable = false, updatable = false)
    private PersonEntity targetUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private SupportActionTypeEnum actionType;

    @Column(nullable = false, updatable = false)
    private String reason;

    @ManyToOne
    @JoinColumn(name = "ticket_id", updatable = false)
    private TicketEntity ticket;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime timestamp;

    @PreUpdate
    private void preventUpdates() {
        throw new IllegalStateException("Les logs d'audit sont immuables et ne peuvent pas être modifiés.");
    }

    @PreRemove
    private void preventDeletes() {
        throw new IllegalStateException("Les logs d'audit sont immuables et ne peuvent pas être supprimés.");
    }
}
