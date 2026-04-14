package com.sni.bokaticowork.features.inventory.catalog.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryTrackingType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_item")
public class InventoryItem {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "item_code", nullable = false, length = 80, unique = true)
    private String itemCode;

    @Column(name = "name", nullable = false, length = 220)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "psku", length = 120, unique = true)
    private String psku;

    @Column(name = "short_code", length = 80, unique = true)
    private String shortCode;

    @Column(name = "display_code", length = 120, unique = true)
    private String displayCode;

    @Column(name = "identification_code", length = 120, unique = true)
    private String identificationCode;

    @Column(name = "specification", columnDefinition = "TEXT")
    private String specification;

    @Column(name = "search_text", columnDefinition = "TEXT")
    private String searchText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private InventoryCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private InventoryUnit unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 40)
    private InventoryItemType itemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "tracking_type", nullable = false, length = 40)
    private InventoryTrackingType trackingType;

    @Column(name = "default_cost")
    private Long defaultCost;

    @Column(name = "sale_price")
    private Long salePrice;

    @Builder.Default
    @Column(name = "taxable", nullable = false)
    private Boolean taxable = Boolean.FALSE;

    @Builder.Default
    @Column(name = "allow_negative_stock", nullable = false)
    private Boolean allowNegativeStock = Boolean.FALSE;

    @Builder.Default
    @Column(name = "requires_expiry_date", nullable = false)
    private Boolean requiresExpiryDate = Boolean.FALSE;

    @Builder.Default
    @Column(name = "requires_lot_number", nullable = false)
    private Boolean requiresLotNumber = Boolean.FALSE;

    @Builder.Default
    @Column(name = "requires_serial_number", nullable = false)
    private Boolean requiresSerialNumber = Boolean.FALSE;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        applyDefaults();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        applyDefaults();
    }

    private void applyDefaults() {
        if (taxable == null) taxable = Boolean.FALSE;
        if (allowNegativeStock == null) allowNegativeStock = Boolean.FALSE;
        if (requiresExpiryDate == null) requiresExpiryDate = Boolean.FALSE;
        if (requiresLotNumber == null) requiresLotNumber = Boolean.FALSE;
        if (requiresSerialNumber == null) requiresSerialNumber = Boolean.FALSE;
        if (active == null) active = Boolean.TRUE;
    }
}
