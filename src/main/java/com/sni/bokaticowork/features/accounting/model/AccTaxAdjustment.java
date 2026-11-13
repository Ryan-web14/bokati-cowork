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

/** Ligne du tableau de passage au resultat fiscal : reintegration ou deduction (lot F15). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_tax_adjustment")
public class AccTaxAdjustment {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "fiscal_year_code")
    private String fiscalYearCode;

    @Column(name = "kind")
    private String kind;

    @Column(name = "code")
    private String code;

    @Column(name = "label")
    private String label;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "comment")
    private String comment;

    @Column(name = "created_by")
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
