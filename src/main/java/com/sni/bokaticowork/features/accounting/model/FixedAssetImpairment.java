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

/** Depreciation d'une immobilisation ou sa reprise (lot F18). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "fixed_asset_impairment")
public class FixedAssetImpairment {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "kind", nullable = false)
    private String kind;

    @Column(name = "impairment_date", nullable = false)
    private LocalDate impairmentDate;

    @Column(name = "indicator")
    private String indicator;

    @Column(name = "current_value", nullable = false)
    private BigDecimal currentValue;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "justification_ref")
    private String justificationRef;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "requested_by", nullable = false)
    private String requestedBy;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "entry_number")
    private String entryNumber;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
