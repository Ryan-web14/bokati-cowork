package com.sni.bokaticowork.features.approval.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Niveau d'une demande : qui doit decider, et ce qui a ete decide. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "approval_step")
public class ApprovalStep {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "level_order", nullable = false)
    private Integer levelOrder;

    @Column(name = "label", nullable = false, length = 120)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "approver_rule", nullable = false, length = 20)
    private com.sni.bokaticowork.features.approval.enums.ApproverRule approverRule;

    /** Decideur attendu (N+1, N+2 ou utilisateur nomme), resolu a la soumission. */
    @Column(name = "expected_approver", length = 180)
    private String expectedApprover;

    @Column(name = "expected_approver_name")
    private String expectedApproverName;

    @Column(name = "permission", length = 80)
    private String permission;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private com.sni.bokaticowork.features.approval.enums.ApprovalStepStatus status;

    @Column(name = "decided_by", length = 180)
    private String decidedBy;

    @Column(name = "decided_by_name")
    private String decidedByName;

    @Column(name = "delegated_from", length = 180)
    private String delegatedFrom;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "comment", length = 1000)
    private String comment;
}
