package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ligne de budget : poste budgetaire et section. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_budget_line")
public class AccBudgetLine {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "budget_id", nullable = false)
    private Long budgetId;

    @Column(name = "item_code", nullable = false)
    private String itemCode;

    @Column(name = "section_code")
    private String sectionCode;

    @Builder.Default
    @Column(name = "annual_amount", nullable = false)
    private java.math.BigDecimal annualAmount = java.math.BigDecimal.ZERO;

    @Column(name = "control_level")
    private String controlLevel;

    @Column(name = "tolerance_pct")
    private java.math.BigDecimal tolerancePct;

    @Column(name = "responsible")
    private String responsible;

    @Builder.Default
    @Column(name = "frozen", nullable = false)
    private Boolean frozen = Boolean.FALSE;

    @Builder.Default
    @Column(name = "alerted_pct", nullable = false)
    private Integer alertedPct = 0;

    @PrePersist
    void prePersist() {
    }
}
