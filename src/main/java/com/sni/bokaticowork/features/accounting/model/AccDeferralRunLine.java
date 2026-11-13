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

/** Besoin d'une ligne a la fin d'une periode (lot F20). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_deferral_run_line")
public class AccDeferralRunLine {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "run_id")
    private Long runId;

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Builder.Default
    @Column(name = "need")
    private BigDecimal need = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "previous")
    private BigDecimal previous = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "difference")
    private BigDecimal difference = BigDecimal.ZERO;
}
