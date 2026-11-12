package com.sni.bokaticowork.features.fund.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ligne de justification d'une avance (lot F6). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "expense_justification_line")
public class ExpenseJustificationLine {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "number", nullable = false)
    private String number;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "expense_date", nullable = false)
    private java.time.LocalDate expenseDate;

    @Column(name = "nature_code", nullable = false)
    private String natureCode;

    @Column(name = "supplier")
    private String supplier;

    @Column(name = "designation", nullable = false)
    private String designation;

    @Column(name = "amount", nullable = false)
    private java.math.BigDecimal amount;

    @Builder.Default
    @Column(name = "vat_amount", nullable = false)
    private java.math.BigDecimal vatAmount = java.math.BigDecimal.ZERO;

    @Column(name = "section")
    private String section;

    @Column(name = "document_reference")
    private String documentReference;

    @Builder.Default
    @Column(name = "honour_statement", nullable = false)
    private Boolean honourStatement = Boolean.FALSE;

    @Column(name = "request_line_no")
    private Integer requestLineNo;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false)
    private com.sni.bokaticowork.features.fund.enums.ExpenseLineStatus status = com.sni.bokaticowork.features.fund.enums.ExpenseLineStatus.PENDING;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "entered_by", nullable = false)
    private String enteredBy;

    @Column(name = "validated_by")
    private String validatedBy;

    @Column(name = "validated_at")
    private java.time.Instant validatedAt;

    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }
}
