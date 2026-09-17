package com.sni.bokaticowork.features.inventory.catalog.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Un article de remplacement propose lorsque l'article demande est indisponible.
 *
 * <p>La relation peut etre declaree reciproque, auquel cas le substitut propose aussi l'article
 * d'origine. Le taux de conversion couvre les equivalents qui ne se remplacent pas a l'unite.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_item_substitute", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_substitute_pair", columnNames = {"item_id", "substitute_item_id"})
})
public class InventoryItemSubstitute {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "substitute_item_id", nullable = false)
    private InventoryItem substituteItem;

    /** Ordre de proposition, le plus petit en premier. */
    @Builder.Default
    @Column(name = "priority", nullable = false)
    private Integer priority = 1;

    /** Quantite de substitut equivalente a une unite de l'article d'origine. */
    @Builder.Default
    @Column(name = "conversion_factor", nullable = false, precision = 19, scale = 6)
    private BigDecimal conversionFactor = BigDecimal.ONE;

    /** Vrai lorsque le substitut propose en retour l'article d'origine. */
    @Builder.Default
    @Column(name = "bidirectional", nullable = false)
    private Boolean bidirectional = Boolean.FALSE;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        if (priority == null) priority = 1;
        if (conversionFactor == null) conversionFactor = BigDecimal.ONE;
        if (bidirectional == null) bidirectional = Boolean.FALSE;
        if (active == null) active = Boolean.TRUE;
    }
}
