package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Delegation d'un reviseur ou superviseur absent : ses revues et visas vont au delegue. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_delegation")
public class CashDelegation {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "delegator", nullable = false, length = 180)
    private String delegator;

    @Column(name = "delegate", nullable = false, length = 180)
    private String delegate;

    @Column(name = "permissions", nullable = false, length = 255)
    private String permissions;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on", nullable = false)
    private LocalDate endsOn;

    @Column(name = "note", length = 500)
    private String note;

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
}
