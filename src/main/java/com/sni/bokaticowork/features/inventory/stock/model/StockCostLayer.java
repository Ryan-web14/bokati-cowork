package com.sni.bokaticowork.features.inventory.stock.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Une couche de cout : ce qu'une entree donnee a reellement coute, et ce qu'il en reste.
 *
 * <p>N'existe que pour les articles valorises en FIFO. Un article en cout moyen pondere n'en cree
 * aucune, ce qui garantit que son comportement reste strictement celui d'avant le lot 2.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock_cost_layer")
public class StockCostLayer {

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

    /** Quantite entree a l'origine, jamais modifiee. */
    @Column(name = "initial_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal initialQuantity;

    /** Quantite non encore consommee. Tombe a zero quand la couche est epuisee. */
    @Column(name = "remaining_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal remainingQuantity;

    /** Cout unitaire de cette entree, en entier XAF. */
    @Column(name = "unit_cost", nullable = false)
    private Long unitCost;

    /** Date d'entree, qui donne l'ordre de consommation et l'anciennete du stock. */
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    /** Mouvement qui a cree la couche, ou null pour une couche d'amorcage. */
    @Column(name = "source_movement_code", length = 90)
    private String sourceMovementCode;

    /** Lot d'origine, lorsque l'article est suivi par lot. */
    @Column(name = "lot_number", length = 120)
    private String lotNumber;

    /**
     * Vrai pour la couche creee lors du passage de l'article au FIFO. Son cout est le cout moyen
     * du moment, pas un cout d'achat reel : le distinguer evite de faire croire a une precision
     * qui n'existe pas.
     */
    @Builder.Default
    @Column(name = "seeded", nullable = false)
    private Boolean seeded = Boolean.FALSE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        if (receivedAt == null) receivedAt = createdAt;
        if (seeded == null) seeded = Boolean.FALSE;
    }

    /** Vrai lorsque la couche est entierement consommee. */
    public boolean isExhausted() {
        return remainingQuantity == null || remainingQuantity.compareTo(BigDecimal.ZERO) <= 0;
    }

    /** Valeur restante de la couche. */
    public BigDecimal remainingValue() {
        if (remainingQuantity == null || unitCost == null) {
            return BigDecimal.ZERO;
        }
        return remainingQuantity.multiply(BigDecimal.valueOf(unitCost));
    }
}
