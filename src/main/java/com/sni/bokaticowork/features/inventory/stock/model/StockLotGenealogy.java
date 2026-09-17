package com.sni.bokaticowork.features.inventory.stock.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.stock.enums.LotGenealogyRelation;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Lien de filiation entre deux lots.
 *
 * <p>Sans cette table, on sait qu un lot est sorti du stock mais pas ce qu il est devenu. C est
 * precisement ce qui manque le jour d un rappel produit ou d un litige fournisseur, quand il faut
 * remonter d un lot suspect vers tout ce qui en descend.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock_lot_genealogy")
public class StockLotGenealogy {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    /** Lot d origine. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_lot_id", nullable = false)
    private StockLot parentLot;

    /** Lot issu du parent. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_lot_id", nullable = false)
    private StockLot childLot;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation", nullable = false, length = 40)
    private LotGenealogyRelation relation;

    /** Quantite du parent qui a servi a produire l enfant. */
    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    /** Mouvement a l origine du lien, lorsqu il y en a un. */
    @Column(name = "source_movement_code", length = 90)
    private String sourceMovementCode;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
