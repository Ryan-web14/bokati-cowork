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

/** Entretien, reparation ou assurance d'une immobilisation (lot F18). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "fixed_asset_maintenance")
public class FixedAssetMaintenance {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "maintenance_date", nullable = false)
    private LocalDate maintenanceDate;

    @Builder.Default
    @Column(name = "kind", nullable = false)
    private String kind = "MAINTENANCE";

    @Column(name = "provider")
    private String provider;

    @Column(name = "contract_ref")
    private String contractRef;

    @Builder.Default
    @Column(name = "cost", nullable = false)
    private BigDecimal cost = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "under_warranty", nullable = false)
    private Boolean underWarranty = Boolean.FALSE;

    @Column(name = "supplier_invoice_number")
    private String supplierInvoiceNumber;

    @Column(name = "description")
    private String description;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
