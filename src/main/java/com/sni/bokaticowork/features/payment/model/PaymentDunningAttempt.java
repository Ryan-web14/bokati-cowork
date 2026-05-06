package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.features.payment.enums.DunningAttemptStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "payment_dunning_attempt", indexes = {
        @Index(name = "idx_dunning_status_scheduled", columnList = "status,scheduled_at"),
        @Index(name = "idx_dunning_intent",           columnList = "payment_intent_id")
})
public class PaymentDunningAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_intent_id", nullable = false)
    private Long paymentIntentId;

    @Column(name = "subscription_number", length = 120)
    private String subscriptionNumber;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private DunningAttemptStatus status = DunningAttemptStatus.PENDING;

    @Column(name = "new_transaction_number", length = 120)
    private String newTransactionNumber;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    public void preUpdate() { updatedAt = Instant.now(); }
}