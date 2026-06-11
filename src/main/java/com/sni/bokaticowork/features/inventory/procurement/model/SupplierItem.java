package com.sni.bokaticowork.features.inventory.procurement.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_supplier_item",
        uniqueConstraints = @UniqueConstraint(columnNames = {"supplier_id", "item_id"}))
public class SupplierItem {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Column(name = "supplier_item_code", length = 120)
    private String supplierItemCode;

    @Column(name = "unit_price")
    private Long unitPrice;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Builder.Default
    @Column(name = "preferred", nullable = false)
    private Boolean preferred = Boolean.FALSE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (active == null) active = Boolean.TRUE;
        if (preferred == null) preferred = Boolean.FALSE;
    }
}
