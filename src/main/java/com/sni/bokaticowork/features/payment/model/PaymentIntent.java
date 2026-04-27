package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "payment_intent", indexes = {
        @Index(name = "idx_payment_intent_number", columnList = "intent_number"),
        @Index(name = "idx_payment_intent_customer", columnList = "customer_type,customer_code"),
        @Index(name = "idx_payment_intent_source", columnList = "source_type,source_code"),
        @Index(name = "idx_payment_intent_status", columnList = "status")
})
public class PaymentIntent {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "intent_number", nullable = false, unique = true, length = 100)
    private String intentNumber;

    @Column(name = "customer_type", nullable = false, length = 60)
    private String customerType;

    @Column(name = "customer_code", nullable = false, length = 120)
    private String customerCode;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private PaymentIntentStatus status;

    @Column(name = "purpose", length = 120)
    private String purpose;

    @Column(name = "source_type", length = 80)
    private String sourceType;

    @Column(name = "source_code", length = 120)
    private String sourceCode;

    @Column(name = "idempotency_key", unique = true, length = 180)
    private String idempotencyKey;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
