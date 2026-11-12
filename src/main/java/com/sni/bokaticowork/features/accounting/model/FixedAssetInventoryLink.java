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

/** Lien facultatif entre une immobilisation et un actif d'inventaire (lot F18). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "fixed_asset_inventory_link")
public class FixedAssetInventoryLink {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "fixed_asset_id", nullable = false)
    private Long fixedAssetId;

    @Column(name = "inventory_asset_code", nullable = false)
    private String inventoryAssetCode;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
