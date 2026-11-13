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

/**
 * Delegation datee (absence, interim). Sans niveau : le delegue agit partout ou le delegant est
 * attendu. Avec un centre et un niveau : il agit sur ce niveau de ce centre (reprise des
 * delegations de la caisse, par niveau).
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "approval_delegation")
public class ApprovalDelegation {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "delegator", nullable = false, length = 180)
    private String delegator;

    @Column(name = "delegate", nullable = false, length = 180)
    private String delegate;

    @Column(name = "center", length = 30)
    private String center;

    @Column(name = "level_order")
    private Integer levelOrder;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on", nullable = false)
    private LocalDate endsOn;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_by", nullable = false, length = 180)
    private String createdBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** Active ce jour-la, et pas annulee. */
    public boolean activeOn(LocalDate day) {
        return cancelledAt == null && !startsOn.isAfter(day) && !endsOn.isBefore(day);
    }
}
