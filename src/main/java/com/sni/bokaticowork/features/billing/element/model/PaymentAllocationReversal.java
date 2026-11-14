package com.sni.bokaticowork.features.billing.element.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** Trace d'une affectation annulee (lot R4) : l'affectation, le sort de la facture, les elements remis en attente. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_allocation_reversal")
public class PaymentAllocationReversal {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "allocation_id", nullable = false)
    private Long allocationId;

    @Column(name = "transaction_number", nullable = false, length = 80)
    private String transactionNumber;

    @Column(name = "document_number", nullable = false, length = 80)
    private String documentNumber;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "allocation_mode", nullable = false, length = 20)
    private String allocationMode;

    @Column(name = "allocated_at")
    private Instant allocatedAt;

    @Column(name = "allocated_by", length = 180)
    private String allocatedBy;

    @Column(name = "reversed_at", nullable = false)
    private Instant reversedAt;

    @Column(name = "reversed_by", length = 180)
    private String reversedBy;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    /** {@code REOPEN} : facture de nouveau due ; {@code NEUTRALIZE} : facture annulee (par avoir si certifiee). */
    @Column(name = "invoice_outcome", nullable = false, length = 20)
    private String invoiceOutcome;

    @Column(name = "elements_json", columnDefinition = "text")
    private String elementsJson;

    /** L'affectation etait deja comptabilisee : la contrepassation D 411 / C 4719 est a passer. */
    @Column(name = "accounting_required", nullable = false)
    private Boolean accountingRequired;

    @PrePersist
    void prePersist() {
        if (reversedAt == null) {
            reversedAt = Instant.now();
        }
        if (accountingRequired == null) {
            accountingRequired = Boolean.FALSE;
        }
    }
}
