package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "pawapay_deposit")
public class PawapayDeposit {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "deposit_id", nullable = false, unique = true, length = 36)
    private String depositId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_intent_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pawapay_deposit_intent"))
    private PaymentIntent paymentIntent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_transaction_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pawapay_deposit_transaction"))
    private PaymentTransaction paymentTransaction;

    @Column(name = "intent_number", nullable = false, length = 100)
    private String intentNumber;

    @Column(name = "transaction_number", nullable = false, length = 100)
    private String transactionNumber;

    @Column(name = "customer_type", nullable = false, length = 60)
    private String customerType;

    @Column(name = "customer_code", nullable = false, length = 120)
    private String customerCode;

    @Column(name = "payer_type", nullable = false, length = 30)
    private String payerType;

    @Column(name = "phone_number", nullable = false, length = 40)
    private String phoneNumber;

    @Column(name = "provider", nullable = false, length = 80)
    private String provider;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "client_reference_id", nullable = false, length = 120)
    private String clientReferenceId;

    @Column(name = "customer_message", nullable = false, length = 22)
    private String customerMessage;

    @Column(name = "callback_url", columnDefinition = "text")
    private String callbackUrl;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "request_payload_json", columnDefinition = "jsonb")
    private String requestPayloadJson;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "provider_response_json", columnDefinition = "jsonb")
    private String providerResponseJson;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "provider_message", columnDefinition = "text")
    private String providerMessage;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Column(name = "last_status_checked_at")
    private Instant lastStatusCheckedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

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
