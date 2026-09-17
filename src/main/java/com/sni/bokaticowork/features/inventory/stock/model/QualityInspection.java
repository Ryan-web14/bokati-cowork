package com.sni.bokaticowork.features.inventory.stock.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.stock.enums.QualityDecision;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Execution d un plan de controle sur un lot donne.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "quality_inspection", uniqueConstraints = {
        @UniqueConstraint(name = "uk_quality_inspection_code", columnNames = "inspection_code")
})
public class QualityInspection {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "inspection_code", nullable = false, length = 80)
    private String inspectionCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private QualityControlPlan plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private StockLot lot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private InventoryLocation location;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 60)
    private StockReferenceType sourceType;

    @Column(name = "source_code", length = 120)
    private String sourceCode;

    @Column(name = "sampled_quantity", precision = 19, scale = 4)
    private BigDecimal sampledQuantity;

    @Column(name = "conform_quantity", precision = 19, scale = 4)
    private BigDecimal conformQuantity;

    @Column(name = "non_conform_quantity", precision = 19, scale = 4)
    private BigDecimal nonConformQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 40)
    private QualityDecision decision;

    @Column(name = "decision_by", length = 120)
    private String decisionBy;

    @Column(name = "decision_reason", columnDefinition = "TEXT")
    private String decisionReason;

    @Column(name = "inspected_by", length = 120)
    private String inspectedBy;

    @Column(name = "inspected_at", nullable = false)
    private Instant inspectedAt;

    /** Non-conformite ouverte par cette inspection, le cas echeant. */
    @Column(name = "non_conformance_code", length = 80)
    private String nonConformanceCode;

    @Builder.Default
    @OneToMany(mappedBy = "inspection", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QualityInspectionResult> results = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (inspectedAt == null) inspectedAt = Instant.now();
    }

    /** Vrai lorsque au moins un critere bloquant est en echec. */
    public boolean hasBlockingFailure() {
        return results.stream().anyMatch(QualityInspectionResult::isBlockingFailure);
    }
}
