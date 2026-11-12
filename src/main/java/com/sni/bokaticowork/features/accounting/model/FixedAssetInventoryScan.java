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

/** Scan d'un bien lors d'une campagne d'inventaire (lot F18). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "fixed_asset_inventory_scan")
public class FixedAssetInventoryScan {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "asset_id")
    private Long assetId;

    @Column(name = "asset_code")
    private String assetCode;

    @Column(name = "unregistered_label")
    private String unregisteredLabel;

    @Column(name = "site")
    private String site;

    @Column(name = "resource_code")
    private String resourceCode;

    @Column(name = "location_label")
    private String locationLabel;

    @Column(name = "photo_ref")
    private String photoRef;

    @Builder.Default
    @Column(name = "mislocated", nullable = false)
    private Boolean mislocated = Boolean.FALSE;

    @Builder.Default
    @Column(name = "holder_alert", nullable = false)
    private Boolean holderAlert = Boolean.FALSE;

    @Column(name = "scanned_by", nullable = false)
    private String scannedBy;

    @Builder.Default
    @Column(name = "scanned_at", nullable = false)
    private Instant scannedAt = Instant.now();
}
