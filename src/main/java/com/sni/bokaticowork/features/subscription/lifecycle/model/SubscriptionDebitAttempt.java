package com.sni.bokaticowork.features.subscription.lifecycle.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Une tentative de prelevement · reussie ou non, elle est ecrite. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "subscription_debit_attempt")
public class SubscriptionDebitAttempt {

    public enum Status {
        SUCCEEDED, INSUFFICIENT_FUNDS, OVER_LIMIT, FAILED
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mandate_id", nullable = false)
    private SubscriptionDebitMandate mandate;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(name = "invoice_number", nullable = false, length = 100)
    private String invoiceNumber;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Status status;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "transaction_number", length = 100)
    private String transactionNumber;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @PrePersist
    public void prePersist() {
        if (executedAt == null) {
            executedAt = Instant.now();
        }
    }
}
