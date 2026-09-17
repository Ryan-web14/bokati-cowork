package com.sni.bokaticowork.features.inventory.stock.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.stock.enums.NonConformanceDisposition;
import com.sni.bokaticowork.features.inventory.stock.enums.NonConformanceSeverity;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Non-conformite constatee sur une marchandise.
 *
 * <p>Porte la decision de traitement et son suivi. Sans elle, un refus de controle se perd dans un
 * champ de texte, et personne ne sait six mois plus tard ce qu est devenue la marchandise.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_non_conformance", uniqueConstraints = {
        @UniqueConstraint(name = "uk_non_conformance_code", columnNames = "non_conformance_code")
})
public class NonConformance {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "non_conformance_code", nullable = false, length = 80)
    private String nonConformanceCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private StockLot lot;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 60)
    private StockReferenceType sourceType;

    @Column(name = "source_code", length = 120)
    private String sourceCode;

    @Column(name = "quantity", precision = 19, scale = 4)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private NonConformanceSeverity severity;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "disposition", nullable = false, length = 40)
    private NonConformanceDisposition disposition = NonConformanceDisposition.PENDING;

    @Column(name = "corrective_action", columnDefinition = "TEXT")
    private String correctiveAction;

    @Column(name = "responsible_code", length = 120)
    private String responsibleCode;

    @Column(name = "due_date")
    private LocalDate dueDate;

    /** Reclamation fournisseur associee, lorsque la cause lui est imputable. */
    @Column(name = "supplier_claim_code", length = 120)
    private String supplierClaimCode;

    /** Cout chiffre de la non-conformite, en entier XAF. */
    @Column(name = "cost_impact")
    private Long costImpact;

    @Column(name = "detected_by", length = 120)
    private String detectedBy;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "closed_by", length = 120)
    private String closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @PrePersist
    void prePersist() {
        if (detectedAt == null) detectedAt = Instant.now();
        if (disposition == null) disposition = NonConformanceDisposition.PENDING;
    }

    public boolean isOpen() {
        return closedAt == null;
    }
}
