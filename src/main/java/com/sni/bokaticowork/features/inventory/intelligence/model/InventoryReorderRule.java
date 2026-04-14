package com.sni.bokaticowork.features.inventory.intelligence.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_reorder_rule", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_reorder_item_location", columnNames = {"item_id", "location_id"})
})
public class InventoryReorderRule {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private InventoryLocation location;

    @Column(name = "min_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal minQuantity;

    @Column(name = "max_quantity", precision = 19, scale = 4)
    private BigDecimal maxQuantity;

    @Column(name = "reorder_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal reorderQuantity;

    @Column(name = "preferred_supplier_code", length = 120)
    private String preferredSupplierCode;

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
        if (active == null) active = Boolean.TRUE;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
