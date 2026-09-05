package com.sellam.store.cash.models;

import com.sellam.store.identity.models.PersonEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="cash_register_sessions", uniqueConstraints = {
        // Unique active session per register constraint. Handled by logic or partial index ideally, 
        // but since we need it at DB level and JPA handles standard uniques, 
        // we'll rely on the service to enforce it correctly alongside DB if possible, 
        // or a custom constraint depending on DB dialect.
})
@Entity
public class CashRegisterSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "register_id", nullable = false)
    private CashRegisterEntity register;

    @ManyToOne(optional = false)
    @JoinColumn(name = "opened_by_id", nullable = false)
    private PersonEntity openedBy;

    @Column(nullable = false)
    private LocalDateTime openedAt;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal openingCashAmount;

    @ManyToOne
    @JoinColumn(name = "closed_by_id")
    private PersonEntity closedBy;

    private LocalDateTime closedAt;

    @Column(precision = 15, scale = 2)
    private BigDecimal closingDeclaredAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal closingComputedAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal discrepancy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CashSessionStatusEnum status;

    @PrePersist
    @PreUpdate
    private void validateAmounts() {
        if (openingCashAmount != null && openingCashAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("openingCashAmount ne peut pas être négatif");
        }
        if (closingDeclaredAmount != null && closingDeclaredAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("closingDeclaredAmount ne peut pas être négatif");
        }
    }
}
