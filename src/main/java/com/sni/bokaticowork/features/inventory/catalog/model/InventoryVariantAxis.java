package com.sni.bokaticowork.features.inventory.catalog.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Un axe de variation d'un modele d'article : taille, couleur, longueur, grade.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_variant_axis", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_axis_template_code", columnNames = {"template_id", "axis_code"})
})
public class InventoryVariantAxis {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private InventoryItemTemplate template;

    @Column(name = "axis_code", nullable = false, length = 40)
    private String axisCode;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Builder.Default
    @Column(name = "position", nullable = false)
    private Integer position = 1;

    @Builder.Default
    @OneToMany(mappedBy = "axis", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<InventoryVariantValue> values = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (position == null) position = 1;
    }
}
