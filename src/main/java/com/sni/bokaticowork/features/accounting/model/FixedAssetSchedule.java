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

/** Dotation mensuelle d'un plan d'amortissement (lot F18). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "fixed_asset_schedule")
public class FixedAssetSchedule {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "kind", nullable = false)
    private String kind;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "period_code", nullable = false)
    private String periodCode;

    @Column(name = "planned", nullable = false)
    private BigDecimal planned;

    @Builder.Default
    @Column(name = "posted", nullable = false)
    private BigDecimal posted = BigDecimal.ZERO;

    @Column(name = "cumulative", nullable = false)
    private BigDecimal cumulative;

    @Column(name = "net_value", nullable = false)
    private BigDecimal netValue;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "PLANNED";

    @Column(name = "entry_number")
    private String entryNumber;

    @Column(name = "posted_in_period")
    private String postedInPeriod;

    @Column(name = "note")
    private String note;

    @Column(name = "posted_at")
    private Instant postedAt;
}
