package com.sni.bokaticowork.features.fund.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Decision datee et signee au fil d'une demande. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "fund_request_approval")
public class FundRequestApproval {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "approver", nullable = false)
    private String approver;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false)
    private com.sni.bokaticowork.features.fund.enums.FundApprovalDecision decision;

    @Column(name = "amount_before")
    private java.math.BigDecimal amountBefore;

    @Column(name = "amount_after")
    private java.math.BigDecimal amountAfter;

    @Column(name = "comment")
    private String comment;

    @Column(name = "delegated_from")
    private String delegatedFrom;

    @Column(name = "decided_at", nullable = false)
    private java.time.Instant decidedAt;

    @PrePersist
    void prePersist() {
        if (decidedAt == null) {
            decidedAt = java.time.Instant.now();
        }
    }
}
