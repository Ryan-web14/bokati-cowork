package com.sni.bokaticowork.features.inventory.stock.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.stock.enums.ProductRecallStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Rappel produit portant sur une plage de numeros de lot.
 *
 * <p>Gele les lots concernes et s appuie sur la tracabilite descendante pour dire ou la marchandise
 * est partie. C est la seule fonction du module ou la lenteur se paie en responsabilite.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_product_recall", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_recall_code", columnNames = "recall_code")
})
public class ProductRecall {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "recall_code", nullable = false, length = 80)
    private String recallCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    /** Premier numero de lot concerne, inclus. Null pour rappeler tous les lots de l article. */
    @Column(name = "lot_number_from", length = 120)
    private String lotNumberFrom;

    /** Dernier numero de lot concerne, inclus. */
    @Column(name = "lot_number_to", length = 120)
    private String lotNumberTo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProductRecallStatus status = ProductRecallStatus.DRAFT;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /** Nombre de lots geles au lancement. */
    @Column(name = "frozen_lot_count")
    private Integer frozenLotCount;

    /** Quantite encore en stock au lancement du rappel. */
    @Column(name = "quantity_in_stock", precision = 19, scale = 4)
    private BigDecimal quantityInStock;

    /** Quantite deja sortie, donc a recuperer aupres des detenteurs. */
    @Column(name = "quantity_issued", precision = 19, scale = 4)
    private BigDecimal quantityIssued;

    /** Quantite effectivement recuperee. */
    @Builder.Default
    @Column(name = "quantity_recovered", precision = 19, scale = 4)
    private BigDecimal quantityRecovered = BigDecimal.ZERO;

    @Column(name = "launched_by", length = 120)
    private String launchedBy;

    @Column(name = "launched_at")
    private Instant launchedAt;

    @Column(name = "closed_by", length = 120)
    private String closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        if (status == null) status = ProductRecallStatus.DRAFT;
        if (quantityRecovered == null) quantityRecovered = BigDecimal.ZERO;
    }

    /** Taux de recuperation, entre zero et un, ou null si rien n etait sorti. */
    public BigDecimal recoveryRate() {
        if (quantityIssued == null || quantityIssued.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return quantityRecovered.divide(quantityIssued, 4, java.math.RoundingMode.HALF_UP);
    }
}
