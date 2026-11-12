package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Etat de rapprochement a une date. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "bank_reconciliation")
public class BankReconciliation {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "bank_account_code", nullable = false)
    private String bankAccountCode;

    @Column(name = "reconciliation_date", nullable = false)
    private java.time.LocalDate reconciliationDate;

    @Column(name = "statement_balance", nullable = false)
    private java.math.BigDecimal statementBalance;

    @Column(name = "book_balance", nullable = false)
    private java.math.BigDecimal bookBalance;

    @Column(name = "outstanding", nullable = false, columnDefinition = "TEXT")
    private String outstanding;

    @Column(name = "difference", nullable = false)
    private java.math.BigDecimal difference;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "DRAFT";

    @Column(name = "prepared_by", nullable = false)
    private String preparedBy;

    @Column(name = "prepared_at", nullable = false)
    private java.time.Instant preparedAt;

    @Column(name = "validated_by")
    private String validatedBy;

    @Column(name = "validated_at")
    private java.time.Instant validatedAt;

    @PrePersist
    void prePersist() {
        if (preparedAt == null) {
            preparedAt = java.time.Instant.now();
        }
    }
}
