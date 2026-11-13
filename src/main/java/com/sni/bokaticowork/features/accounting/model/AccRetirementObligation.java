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
import java.time.LocalDate;

/** Calcul ou import des indemnites de fin de carriere d'un exercice (lot F19). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_retirement_obligation")
public class AccRetirementObligation {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "fiscal_year")
    private String fiscalYear;

    @Column(name = "computed_at")
    private LocalDate computedAt;

    @Column(name = "method")
    private String method;

    @Column(name = "assumptions")
    private String assumptions;

    @Builder.Default
    @Column(name = "headcount")
    private Integer headcount = 0;

    @Builder.Default
    @Column(name = "amount")
    private BigDecimal amount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "previous")
    private BigDecimal previous = BigDecimal.ZERO;

    @Column(name = "detail")
    private String detail;

    @Column(name = "entry_number")
    private String entryNumber;

    @Column(name = "created_by")
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
