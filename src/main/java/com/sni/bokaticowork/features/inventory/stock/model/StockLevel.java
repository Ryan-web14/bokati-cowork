package com.sni.bokaticowork.features.inventory.stock.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
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
@Table(name = "stock_level", uniqueConstraints = {
        @UniqueConstraint(name = "uk_stock_level_item_location", columnNames = {"item_id", "location_id"})
})
public class StockLevel {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private InventoryLocation location;

    @Builder.Default
    @Column(name = "quantity_on_hand", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "quantity_reserved", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityReserved = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "quantity_available", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityAvailable = BigDecimal.ZERO;

    @Column(name = "average_cost")
    private Long averageCost;

    @Column(name = "last_movement_at")
    private Instant lastMovementAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        recalculateAvailable();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        recalculateAvailable();
    }

    public void recalculateAvailable() {
        if (quantityOnHand == null) quantityOnHand = BigDecimal.ZERO;
        if (quantityReserved == null) quantityReserved = BigDecimal.ZERO;
        quantityAvailable = quantityOnHand.subtract(quantityReserved);
    }
}
