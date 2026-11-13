package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Lot C0 : unite de vente du catalogue (heure, jour, mois, personne, forfait...). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "billing_catalog_unit")
public class CatalogUnit {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "plural_label", length = 100)
    private String pluralLabel;

    @Column(name = "symbol", length = 20)
    private String symbol;

    /** Decimales permises sur la quantite (0 a 4). */
    @Column(name = "quantity_scale", nullable = false)
    @Builder.Default
    private Integer quantityScale = 2;

    /** Unite d'inventaire correspondante, pour les articles relies a l'inventaire (decision CD1). */
    @Column(name = "inventory_unit_code", length = 50)
    private String inventoryUnitCode;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_from_item", nullable = false)
    @Builder.Default
    private Boolean createdFromItem = Boolean.FALSE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
