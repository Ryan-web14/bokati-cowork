package com.sni.bokaticowork.features.inventory.asset.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetAssigneeType;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetCondition;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetStatus;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "asset")
public class Asset {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "asset_code", nullable = false, length = 90, unique = true)
    private String assetCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Column(name = "serial_number", length = 160, unique = true)
    private String serialNumber;

    @Column(name = "asset_tag", length = 120, unique = true)
    private String assetTag;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private AssetStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", nullable = false, length = 40)
    private AssetCondition condition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private InventoryLocation location;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_to_type", length = 40)
    private AssetAssigneeType assignedToType;

    @Column(name = "assigned_to_code", length = 120)
    private String assignedToCode;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_cost")
    private Long purchaseCost;

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    @Column(name = "useful_life_months")
    private Integer usefulLifeMonths;

    @Column(name = "residual_value")
    private Long residualValue;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = AssetStatus.AVAILABLE;
        if (condition == null) condition = AssetCondition.GOOD;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
