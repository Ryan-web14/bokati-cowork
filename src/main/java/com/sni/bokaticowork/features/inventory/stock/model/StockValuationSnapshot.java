package com.sni.bokaticowork.features.inventory.stock.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.stock.enums.ValuationMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Photo de la valeur du stock a une date, pour un couple article et emplacement.
 *
 * <p>Sans ces photos, reconstituer la valeur du stock au 31 decembre suppose de rejouer tout le
 * journal, ce qui devient impraticable et discutable des que la methode de valorisation a change.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock_valuation_snapshot", uniqueConstraints = {
        @UniqueConstraint(name = "uk_stock_snapshot_unique",
                columnNames = {"snapshot_date", "item_code", "location_code"})
})
public class StockValuationSnapshot {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "period_code", length = 40)
    private String periodCode;

    @Column(name = "item_code", nullable = false, length = 80)
    private String itemCode;

    @Column(name = "location_code", nullable = false, length = 80)
    private String locationCode;

    @Column(name = "quantity_on_hand", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityOnHand;

    @Column(name = "unit_cost")
    private Long unitCost;

    @Column(name = "total_value", nullable = false)
    private Long totalValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "valuation_method", nullable = false, length = 30)
    private ValuationMethod valuationMethod;

    /** Anciennete de la plus vieille couche restante, en jours. Null hors FIFO. */
    @Column(name = "oldest_layer_age_days")
    private Integer oldestLayerAgeDays;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
