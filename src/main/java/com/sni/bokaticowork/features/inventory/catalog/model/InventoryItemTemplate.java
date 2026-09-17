package com.sni.bokaticowork.features.inventory.catalog.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryTrackingType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Modele d'article a variantes : une vis M8 declinee en trois longueurs, un vetement en cinq tailles.
 *
 * <p>Le modele ne porte pas de stock. Chaque variante generee est un {@link InventoryItem} a part
 * entiere, ce qui evite de toucher a la mecanique de stock existante.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_item_template", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_template_code", columnNames = "template_code")
})
public class InventoryItemTemplate {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "template_code", nullable = false, length = 80)
    private String templateCode;

    @Column(name = "name", nullable = false, length = 220)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

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
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Builder.Default
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<InventoryVariantAxis> axes = new ArrayList<>();

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
        if (active == null) active = Boolean.TRUE;
    }
}
