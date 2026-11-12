package com.sni.bokaticowork.features.fund.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Delegation de validation en cas d'absence. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "fund_approval_delegation")
public class FundApprovalDelegation {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "delegator", nullable = false)
    private String delegator;

    @Column(name = "delegate", nullable = false)
    private String delegate;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "starts_on", nullable = false)
    private java.time.LocalDate startsOn;

    @Column(name = "ends_on", nullable = false)
    private java.time.LocalDate endsOn;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "cancelled_at")
    private java.time.Instant cancelledAt;

    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }
}
