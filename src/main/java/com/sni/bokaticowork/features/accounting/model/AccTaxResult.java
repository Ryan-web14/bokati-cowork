package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/** Resultat fiscal arrete d'un exercice, avec le report des deficits et l'impot (lot F15). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_tax_result")
public class AccTaxResult {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "fiscal_year_code")
    private String fiscalYearCode;

    @Column(name = "accounting_result")
    private BigDecimal accountingResult;

    @Column(name = "reintegrations")
    private BigDecimal reintegrations;

    @Column(name = "deductions")
    private BigDecimal deductions;

    @Builder.Default
    @Column(name = "losses_used")
    private BigDecimal lossesUsed = BigDecimal.ZERO;

    @Column(name = "tax_result")
    private BigDecimal taxResult;

    @Builder.Default
    @Column(name = "loss_carried_forward")
    private BigDecimal lossCarriedForward = BigDecimal.ZERO;

    @Column(name = "rate")
    private BigDecimal rate;

    @Column(name = "tax")
    private BigDecimal tax;

    @Builder.Default
    @Column(name = "minimum_tax")
    private BigDecimal minimumTax = BigDecimal.ZERO;

    @Column(name = "tax_due")
    private BigDecimal taxDue;

    @Column(name = "entry_number")
    private String entryNumber;

    @Builder.Default
    @Column(name = "status")
    private String status = "DRAFT";

    @Column(name = "computed_by")
    private String computedBy;

    @Builder.Default
    @Column(name = "computed_at")
    private Instant computedAt = Instant.now();
}
