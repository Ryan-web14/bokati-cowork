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

import java.time.Instant;

/** Etape de la liste de cloture d'un exercice (lot F12). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_closing_step")
public class AccClosingStep {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "fiscal_year_code")
    private String fiscalYearCode;

    @Column(name = "step_code")
    private String stepCode;

    @Builder.Default
    @Column(name = "status")
    private String status = "TODO";

    @Column(name = "detail")
    private String detail;

    @Column(name = "checked_by")
    private String checkedBy;

    @Column(name = "checked_at")
    private Instant checkedAt;

    @Column(name = "comment")
    private String comment;
}
