package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Poste budgetaire defini par l'utilisateur (lot F16). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_budget_item")
public class AccBudgetItem {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "parent_code")
    private String parentCode;

    @Builder.Default
    @Column(name = "item_type", nullable = false)
    private String itemType = "CHARGE";

    @Column(name = "responsible")
    private String responsible;

    @Builder.Default
    @Column(name = "control_level", nullable = false)
    private String controlLevel = "ALERT";

    @Builder.Default
    @Column(name = "tolerance_pct", nullable = false)
    private java.math.BigDecimal tolerancePct = java.math.BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "control_at_parent", nullable = false)
    private Boolean controlAtParent = Boolean.FALSE;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "merged_into")
    private String mergedInto;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }
}
