package com.sni.bokaticowork.features.inventory.procurement.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.procurement.enums.PurchaseApprovalLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "purchase_approval_step")
public class PurchaseApprovalStep {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_level", nullable = false, length = 40)
    private PurchaseApprovalLevel approvalLevel;

    @Column(name = "approved_by", nullable = false, length = 120)
    private String approvedBy;

    @Column(name = "approved_at", nullable = false)
    private Instant approvedAt;

    @PrePersist
    void prePersist() {
        if (approvedAt == null) approvedAt = Instant.now();
    }
}
