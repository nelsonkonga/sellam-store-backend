package com.sellam.store.payments.models;

import com.sellam.store.shops.models.ShopEntity;
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
 * Trace chaque tentative de paiement d'abonnement via CinetPay.
 *
 * transactionId : identifiant unique généré par nos soins (transmis à
 * CinetPay comme "transaction_id"), sert de clé d'idempotence : un webhook
 * reçu plusieurs fois pour le même transactionId ne doit confirmer
 * l'abonnement qu'une seule fois (cf. PaymentService.handleNotification).
 *
 * cinetpayPaymentToken : token retourné par CinetPay à l'initiation,
 * nécessaire côté frontend pour ouvrir le widget Seamless.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "payment_transactions")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class PaymentTransactionEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private ShopEntity shop;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PaymentTransactionStatusEnum status = PaymentTransactionStatusEnum.PENDING;

    @Column(name = "cinetpay_payment_token", columnDefinition = "TEXT")
    private String cinetpayPaymentToken;

    @Column(name = "cinetpay_operator_id")
    private String cinetpayOperatorId;

    @Column(name = "cinetpay_payment_method")
    private String cinetpayPaymentMethod;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
