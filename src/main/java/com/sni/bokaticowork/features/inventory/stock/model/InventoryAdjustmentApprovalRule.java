package com.sni.bokaticowork.features.inventory.stock.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.procurement.enums.PurchaseApprovalLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Seuil au-dela duquel un ajustement de stock exige un visa hierarchique.
 *
 * <p>Meme modele que les regles d'approbation d'achat, deja en place et eprouvees : une tranche de
 * montant, un niveau requis.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_adjustment_approval_rule")
public class InventoryAdjustmentApprovalRule {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_level", nullable = false, length = 40)
    private PurchaseApprovalLevel approvalLevel;

    /** Borne basse de la tranche, en valeur absolue de l'ecart, en entier XAF. */
    @Column(name = "min_amount", nullable = false)
    private Long minAmount;

    /** Borne haute, ou null pour une tranche ouverte. */
    @Column(name = "max_amount")
    private Long maxAmount;

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

    /** Vrai si le montant tombe dans cette tranche. */
    public boolean covers(long amount) {
        return amount >= minAmount && (maxAmount == null || amount <= maxAmount);
    }
}
