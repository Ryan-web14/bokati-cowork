package com.sni.bokaticowork.features.subscription.subscription.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementTransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
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
@Table(name = "entitlement_ledger", indexes = {
        @Index(name = "idx_entitlement_ledger_grant", columnList = "grant_id"),
        @Index(name = "idx_entitlement_ledger_reference", columnList = "reference_type,reference_id"),
        @Index(name = "idx_entitlement_ledger_idempotency", columnList = "idempotency_key")
})
public class EntitlementLedger {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_entitlement_ledger_grant"))
    private EntitlementGrant grant;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 40)
    private EntitlementTransactionType transactionType;

    @Column(name = "quantity", precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "before_quantity", precision = 19, scale = 4)
    private BigDecimal beforeQuantity;

    @Column(name = "after_quantity", precision = 19, scale = 4)
    private BigDecimal afterQuantity;

    @Column(name = "reference_type", length = 80)
    private String referenceType;

    @Column(name = "reference_id", length = 120)
    private String referenceId;

    @Column(name = "idempotency_key", length = 160)
    private String idempotencyKey;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
