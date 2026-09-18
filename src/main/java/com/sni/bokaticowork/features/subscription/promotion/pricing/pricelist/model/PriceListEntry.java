package com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.TargetScope;
import com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.enums.PriceMode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Une ligne de grille · ce que coute un objet donne, pour cette grille.
 *
 * <p>Le palier de quantite est ce qui rend le degressif possible sans table supplementaire. Trois
 * lignes sur le meme objet, a partir d'un poste, de cinq et de dix, et la ligne retenue est celle
 * dont le palier est le plus eleve parmi ceux que la quantite atteint.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "price_list_entry", indexes = {
        @Index(name = "idx_price_list_entry_list", columnList = "price_list_id"),
        @Index(name = "idx_price_list_entry_target", columnList = "target_scope,target_code,min_quantity")
})
public class PriceListEntry {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "price_list_id", nullable = false)
    private PriceList priceList;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_scope", nullable = false, length = 40)
    private TargetScope targetScope;

    /** Code de l'objet tarife, nul pour une ligne qui couvre toute une portee. */
    @Column(name = "target_code", length = 120)
    private String targetCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_mode", nullable = false, length = 40)
    private PriceMode priceMode;

    @Column(name = "value", nullable = false, precision = 19, scale = 4)
    private BigDecimal value;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "min_quantity", nullable = false)
    @Builder.Default
    private Integer minQuantity = 1;

    /** Tarif propre a un rythme de facturation, nul si la ligne vaut pour tous. */
    @Column(name = "billing_cycle", length = 40)
    private String billingCycle;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
