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

/** Periode mensuelle d'un exercice. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_period")
public class AccPeriod {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "fiscal_year_id", nullable = false)
    private Long fiscalYearId;

    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private com.sni.bokaticowork.features.accounting.enums.PeriodStatus status;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "locked_by", length = 180)
    private String lockedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by", length = 180)
    private String closedBy;
}
