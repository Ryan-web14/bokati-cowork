package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Echeance d'un tableau d'amortissement, par version (lot F17). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_loan_installment")
public class AccLoanInstallment {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "number", nullable = false)
    private Integer number;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "opening_balance", nullable = false)
    private BigDecimal openingBalance;

    @Column(name = "interest", nullable = false)
    private BigDecimal interest;

    @Column(name = "principal", nullable = false)
    private BigDecimal principal;

    @Builder.Default
    @Column(name = "insurance", nullable = false)
    private BigDecimal insurance = BigDecimal.ZERO;

    @Column(name = "payment", nullable = false)
    private BigDecimal payment;

    @Column(name = "closing_balance", nullable = false)
    private BigDecimal closingBalance;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "PLANNED";

    @Column(name = "entry_number")
    private String entryNumber;

    @Column(name = "posted_at")
    private Instant postedAt;
}
