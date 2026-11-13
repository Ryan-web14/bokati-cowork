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
@Table(name = "payment_allocation")
public class PaymentAllocation {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_transaction_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payment_allocation_transaction"))
    private PaymentTransaction paymentTransaction;

    @Column(name = "billing_document_number", nullable = false, length = 120)
    private String billingDocumentNumber;

    @Column(name = "allocated_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal allocatedAmount;

    @Column(name = "allocated_at", nullable = false)
    private Instant allocatedAt;

    /**
     * Lot C9 (facturation) : PAYMENT pour une imputation faite avec le paiement, LATE pour une
     * affectation apres coup d'une somme en attente (ecriture propre 4719 vers 411).
     */
    @Column(name = "allocation_mode", nullable = false, length = 20)
    @Builder.Default
    private String allocationMode = "PAYMENT";

    @Column(name = "allocated_by", length = 180)
    private String allocatedBy;

    @PrePersist
    public void prePersist() {
        allocatedAt = Instant.now();
    }
}
