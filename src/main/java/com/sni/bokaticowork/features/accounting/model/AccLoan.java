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

/** Emprunt (lot F17). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_loan")
public class AccLoan {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "lender_type", nullable = false)
    private String lenderType;

    @Column(name = "lender_code")
    private String lenderCode;

    @Column(name = "lender_name", nullable = false)
    private String lenderName;

    @Column(name = "purpose")
    private String purpose;

    @Column(name = "principal", nullable = false)
    private BigDecimal principal;

    @Builder.Default
    @Column(name = "currency", nullable = false)
    private String currency = "XAF";

    @Column(name = "disbursed_on", nullable = false)
    private LocalDate disbursedOn;

    @Column(name = "first_due_date", nullable = false)
    private LocalDate firstDueDate;

    @Builder.Default
    @Column(name = "rate", nullable = false)
    private BigDecimal rate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "rate_type", nullable = false)
    private String rateType = "FIXED";

    @Builder.Default
    @Column(name = "day_count", nullable = false)
    private String dayCount = "30_360";

    @Builder.Default
    @Column(name = "frequency_months", nullable = false)
    private Integer frequencyMonths = 1;

    @Column(name = "term_count", nullable = false)
    private Integer termCount;

    @Builder.Default
    @Column(name = "deferral_type", nullable = false)
    private String deferralType = "NONE";

    @Builder.Default
    @Column(name = "deferral_count", nullable = false)
    private Integer deferralCount = 0;

    @Builder.Default
    @Column(name = "method", nullable = false)
    private String method = "ANNUITY";

    @Builder.Default
    @Column(name = "fees", nullable = false)
    private BigDecimal fees = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "insurance", nullable = false)
    private BigDecimal insurance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "loan_account", nullable = false)
    private String loanAccount = "162";

    @Builder.Default
    @Column(name = "interest_account", nullable = false)
    private String interestAccount = "671";

    @Builder.Default
    @Column(name = "insurance_account", nullable = false)
    private String insuranceAccount = "625";

    @Builder.Default
    @Column(name = "fees_account", nullable = false)
    private String feesAccount = "631";

    @Builder.Default
    @Column(name = "accrued_account", nullable = false)
    private String accruedAccount = "166";

    @Column(name = "bank_account_code", nullable = false)
    private String bankAccountCode;

    @Column(name = "section_code")
    private String sectionCode;

    @Column(name = "budget_item_code")
    private String budgetItemCode;

    @Builder.Default
    @Column(name = "posting_mode", nullable = false)
    private String postingMode = "AT_DATE";

    @Builder.Default
    @Column(name = "schedule_version", nullable = false)
    private Integer scheduleVersion = 1;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "DRAFT";

    @Column(name = "disbursement_entry")
    private String disbursementEntry;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;
}
