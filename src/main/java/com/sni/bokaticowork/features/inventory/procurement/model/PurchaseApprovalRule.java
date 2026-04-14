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
@Table(name = "purchase_approval_rule")
public class PurchaseApprovalRule {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_level", nullable = false, unique = true, length = 40)
    private PurchaseApprovalLevel approvalLevel;

    @Column(name = "min_amount", nullable = false)
    private Long minAmount;

    @Column(name = "max_amount")
    private Long maxAmount;

    @Column(name = "required_approvals", nullable = false)
    private Integer requiredApprovals;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (active == null) active = true;
        if (requiredApprovals == null || requiredApprovals < 1) requiredApprovals = 1;
        if (createdAt == null) createdAt = Instant.now();
    }
}
