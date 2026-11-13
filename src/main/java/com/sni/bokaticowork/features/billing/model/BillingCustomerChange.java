package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/** Changement de client sur une facture et les pieces qu'il a produites (lot B6). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "billing_customer_change")
public class BillingCustomerChange {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "original_document_number", nullable = false, length = 60)
    private String originalDocumentNumber;

    @Column(name = "new_document_number", length = 60)
    private String newDocumentNumber;

    /** IN_PLACE (nouvelle version) ou REISSUE (avoirs et nouvelle facture). */
    @Column(name = "mode", nullable = false, length = 20)
    private String mode;

    @Column(name = "unpaid_credit_note", length = 60)
    private String unpaidCreditNote;

    @Column(name = "paid_credit_note", length = 60)
    private String paidCreditNote;

    @Column(name = "from_type", length = 30)
    private String fromType;

    @Column(name = "from_code", length = 80)
    private String fromCode;

    @Column(name = "to_type", nullable = false, length = 30)
    private String toType;

    @Column(name = "to_code", nullable = false, length = 80)
    private String toCode;

    @Column(name = "payments_mode", length = 20)
    private String paymentsMode;

    @Column(name = "transferred_amount", precision = 19, scale = 4)
    private BigDecimal transferredAmount;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "created_by", nullable = false, length = 180)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
