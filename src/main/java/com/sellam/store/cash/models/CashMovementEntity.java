package com.sellam.store.cash.models;

import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.invoices.models.InvoiceEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="cash_movements")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class CashMovementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id", nullable = false, updatable = false)
    private CashRegisterSessionEntity session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private CashMovementTypeEnum type;

    @Column(nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(updatable = false)
    private String reason;

    @ManyToOne
    @JoinColumn(name = "reference_invoice_id", updatable = false)
    private InvoiceEntity referenceInvoice;

    @ManyToOne(optional = false)
    @JoinColumn(name = "effectue_par_id", nullable = false, updatable = false)
    private PersonEntity effectuePar;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime timestamp;

    @PreUpdate
    private void preventUpdates() {
        throw new IllegalStateException("Les CashMovementEntity sont immuables et ne peuvent pas être modifiés.");
    }

    @PreRemove
    private void preventDeletes() {
        throw new IllegalStateException("Les CashMovementEntity sont immuables et ne peuvent pas être supprimés.");
    }
}
