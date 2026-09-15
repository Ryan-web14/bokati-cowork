package com.sni.bokaticowork.features.inventory.catalog.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryPriceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Trace d'un changement de prix, alimentee automatiquement a chaque modification d'article.
 *
 * <p>Sans cet historique, une variation de marge est inexplicable apres coup.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_item_price_history")
public class InventoryItemPriceHistory {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_type", nullable = false, length = 30)
    private InventoryPriceType priceType;

    @Column(name = "previous_value")
    private Long previousValue;

    @Column(name = "new_value")
    private Long newValue;

    @Column(name = "changed_by", length = 120)
    private String changedBy;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @PrePersist
    void prePersist() {
        if (changedAt == null) changedAt = Instant.now();
    }
}
