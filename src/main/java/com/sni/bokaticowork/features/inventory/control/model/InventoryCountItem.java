package com.sni.bokaticowork.features.inventory.control.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_count_item", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_count_item", columnNames = {"inventory_count_id", "item_id"})
})
public class InventoryCountItem {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_count_id", nullable = false)
    private InventoryCount inventoryCount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Column(name = "expected_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal expectedQuantity;

    @Column(name = "counted_quantity", precision = 19, scale = 4)
    private BigDecimal countedQuantity;

    @Column(name = "variance_quantity", precision = 19, scale = 4)
    private BigDecimal varianceQuantity;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public void recalculateVariance() {
        if (countedQuantity == null || expectedQuantity == null) {
            varianceQuantity = null;
            return;
        }
        varianceQuantity = countedQuantity.subtract(expectedQuantity);
    }
}
