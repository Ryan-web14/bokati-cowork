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

/** Campagne d'inventaire physique des immobilisations (lot F18). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "fixed_asset_inventory_campaign")
public class FixedAssetInventoryCampaign {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "site")
    private String site;

    @Column(name = "resource_codes")
    private String resourceCodes;

    @Column(name = "category_codes")
    private String categoryCodes;

    @Column(name = "team", nullable = false)
    private String team;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "OPEN";

    @Builder.Default
    @Column(name = "expected_count", nullable = false)
    private Integer expectedCount = 0;

    @Builder.Default
    @Column(name = "seen_count", nullable = false)
    private Integer seenCount = 0;

    @Builder.Default
    @Column(name = "missing_count", nullable = false)
    private Integer missingCount = 0;

    @Builder.Default
    @Column(name = "found_count", nullable = false)
    private Integer foundCount = 0;

    @Builder.Default
    @Column(name = "mislocated_count", nullable = false)
    private Integer mislocatedCount = 0;

    @Column(name = "missing_codes")
    private String missingCodes;

    @Column(name = "mislocated_codes")
    private String mislocatedCodes;

    @Column(name = "decisions")
    private String decisions;

    @Column(name = "witness")
    private String witness;

    @Column(name = "voucher_number")
    private String voucherNumber;

    @Column(name = "opened_by", nullable = false)
    private String openedBy;

    @Column(name = "closed_by")
    private String closedBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;
}
