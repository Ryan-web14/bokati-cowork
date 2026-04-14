package com.sni.bokaticowork.features.inventory.catalog.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
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
@Table(name = "inventory_unit_conversion")
public class InventoryUnitConversion {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Column(name = "from_unit_code", nullable = false, length = 80)
    private String fromUnitCode;

    @Column(name = "to_unit_code", nullable = false, length = 80)
    private String toUnitCode;

    @Column(name = "factor", nullable = false, precision = 19, scale = 6)
    private BigDecimal factor;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (active == null) active = true;
        if (createdAt == null) createdAt = Instant.now();
    }
}
