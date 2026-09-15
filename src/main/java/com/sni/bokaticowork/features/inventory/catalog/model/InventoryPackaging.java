package com.sni.bokaticowork.features.inventory.catalog.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryPackagingLevel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Un conditionnement d'un article : ce que contient un carton, une palette, un blister.
 *
 * <p>Sert au calcul de chargement, a l'etiquetage logistique et aux arrondis de commande, qui
 * doivent respecter les multiples du fournisseur.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_packaging", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_packaging_item_level", columnNames = {"item_id", "packaging_level"})
})
public class InventoryPackaging {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Enumerated(EnumType.STRING)
    @Column(name = "packaging_level", nullable = false, length = 30)
    private InventoryPackagingLevel packagingLevel;

    @Column(name = "name", length = 120)
    private String name;

    /** Nombre d'unites de base contenues dans ce conditionnement. */
    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "barcode_value", length = 120)
    private String barcodeValue;

    @Column(name = "weight_kg", precision = 19, scale = 4)
    private BigDecimal weightKg;

    @Column(name = "length_mm")
    private Integer lengthMm;

    @Column(name = "width_mm")
    private Integer widthMm;

    @Column(name = "height_mm")
    private Integer heightMm;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        if (active == null) active = Boolean.TRUE;
    }
}
