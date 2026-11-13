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

/** Version d'un plan d'amortissement, comptable ou fiscal (lot F18). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "fixed_asset_plan")
public class FixedAssetPlan {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "kind", nullable = false)
    private String kind;

    @Column(name = "method", nullable = false)
    private String method;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    @Column(name = "rate")
    private BigDecimal rate;

    @Column(name = "coefficient")
    private BigDecimal coefficient;

    @Column(name = "base", nullable = false)
    private BigDecimal base;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
