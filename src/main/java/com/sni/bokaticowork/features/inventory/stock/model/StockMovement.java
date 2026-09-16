package com.sni.bokaticowork.features.inventory.stock.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.stock.enums.StockMovementType;
import com.sni.bokaticowork.features.inventory.stock.enums.StockOutReasonCode;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock_movement")
public class StockMovement {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "movement_code", nullable = false, length = 90, unique = true)
    private String movementCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_from_id")
    private InventoryLocation locationFrom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_to_id")
    private InventoryLocation locationTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 40)
    private StockMovementType movementType;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_cost")
    private Long unitCost;

    @Column(name = "total_cost")
    private Long totalCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", length = 60)
    private StockReferenceType referenceType;

    @Column(name = "reference_code", length = 120)
    private String referenceCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 40)
    private StockOutReasonCode reasonCode;

    /** Motif d ajustement codifie, qui porte le compte de contrepartie comptable. */
    @Column(name = "adjustment_reason_code", length = 40)
    private String adjustmentReasonCode;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "allow_negative_override", nullable = false)
    private Boolean allowNegativeOverride;

    @Builder.Default
    @Column(name = "reversed", nullable = false)
    private Boolean reversed = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_movement_id")
    private StockMovement reversalOfMovement;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @Column(name = "reversed_by", length = 120)
    private String reversedBy;

    @Column(name = "reversal_reason", columnDefinition = "TEXT")
    private String reversalReason;

    @Column(name = "performed_by", length = 120)
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @PrePersist
    void prePersist() {
        if (performedAt == null) performedAt = Instant.now();
        if (allowNegativeOverride == null) allowNegativeOverride = Boolean.FALSE;
        if (reversed == null) reversed = Boolean.FALSE;
        if (unitCost != null && quantity != null) totalCost = quantity.multiply(java.math.BigDecimal.valueOf(unitCost)).longValue();
    }
}
