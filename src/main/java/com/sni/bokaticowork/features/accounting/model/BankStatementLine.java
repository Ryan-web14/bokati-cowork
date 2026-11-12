package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ligne de releve (montant signe : entree positive). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "bank_statement_line")
public class BankStatementLine {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "statement_id", nullable = false)
    private Long statementId;

    @Column(name = "bank_account_code", nullable = false)
    private String bankAccountCode;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "operation_date", nullable = false)
    private java.time.LocalDate operationDate;

    @Column(name = "value_date")
    private java.time.LocalDate valueDate;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "reference")
    private String reference;

    @Column(name = "amount", nullable = false)
    private java.math.BigDecimal amount;

    @Column(name = "fingerprint", nullable = false)
    private String fingerprint;

    @Column(name = "matching_code")
    private String matchingCode;

    @Column(name = "matched_at")
    private java.time.Instant matchedAt;

    @Column(name = "matched_by")
    private String matchedBy;

    @Column(name = "awaiting_payment")
    private String awaitingPayment;

    @Column(name = "posted_entry")
    private String postedEntry;

    @PrePersist
    void prePersist() {
    }
}
