package com.sni.bokaticowork.features.inventory.catalog.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.*;

/**
 * Une valeur possible sur un axe de variation : M, L, XL pour l'axe taille.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_variant_value", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_variant_value_code", columnNames = {"axis_id", "value_code"})
})
public class InventoryVariantValue {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "axis_id", nullable = false)
    private InventoryVariantAxis axis;

    @Column(name = "value_code", nullable = false, length = 40)
    private String valueCode;

    @Column(name = "label", nullable = false, length = 120)
    private String label;

    @Builder.Default
    @Column(name = "position", nullable = false)
    private Integer position = 1;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @PrePersist
    void prePersist() {
        if (position == null) position = 1;
        if (active == null) active = Boolean.TRUE;
    }
}
