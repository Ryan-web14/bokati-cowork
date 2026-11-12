package com.sni.bokaticowork.features.accounting.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Categorie d'immobilisation et ses comptes (lot F18). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "fa_category")
public class FaCategory {

    @Id
    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "asset_account", nullable = false)
    private String assetAccount;

    @Column(name = "depreciation_account")
    private String depreciationAccount;

    @Column(name = "expense_account")
    private String expenseAccount;

    @Column(name = "impairment_account")
    private String impairmentAccount;

    @Column(name = "impairment_expense_account")
    private String impairmentExpenseAccount;

    @Column(name = "impairment_reversal_account")
    private String impairmentReversalAccount;

    @Builder.Default
    @Column(name = "disposal_value_account", nullable = false)
    private String disposalValueAccount = "812";

    @Builder.Default
    @Column(name = "disposal_income_account", nullable = false)
    private String disposalIncomeAccount = "822";

    @Builder.Default
    @Column(name = "method", nullable = false)
    private String method = "LINEAR";

    @Column(name = "duration_months")
    private Integer durationMonths;

    @Column(name = "coefficient")
    private BigDecimal coefficient;

    /** Taux lineaire annuel en pourcentage : quand il est renseigne, la duree en decoule (15 % : 80 mois). */
    @Column(name = "annual_rate")
    private BigDecimal annualRate;

    @Builder.Default
    @Column(name = "threshold", nullable = false)
    private BigDecimal threshold = new BigDecimal("100000");

    @Builder.Default
    @Column(name = "tracking", nullable = false)
    private String tracking = "UNIT";

    @Builder.Default
    @Column(name = "track_off_books", nullable = false)
    private Boolean trackOffBooks = Boolean.FALSE;

    @Column(name = "tax_method")
    private String taxMethod;

    @Column(name = "tax_duration_months")
    private Integer taxDurationMonths;

    @Column(name = "component_types")
    private String componentTypes;

    @Builder.Default
    @Column(name = "vat_recoverable", nullable = false)
    private Boolean vatRecoverable = Boolean.TRUE;

    @Builder.Default
    @Column(name = "in_progress", nullable = false)
    private Boolean inProgress = Boolean.FALSE;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;
}
