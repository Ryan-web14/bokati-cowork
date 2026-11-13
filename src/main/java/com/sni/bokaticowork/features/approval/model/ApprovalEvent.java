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

/** Evenement date et signe de la frise d'une demande. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "approval_event")
public class ApprovalEvent {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "level_order")
    private Integer levelOrder;

    @Column(name = "actor", nullable = false, length = 180)
    private String actor;

    @Column(name = "actor_name")
    private String actorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "event", nullable = false, length = 30)
    private com.sni.bokaticowork.features.approval.enums.ApprovalEventType event;

    @Column(name = "comment", length = 1000)
    private String comment;

    @Column(name = "amount_before", precision = 19, scale = 4)
    private BigDecimal amountBefore;

    @Column(name = "amount_after", precision = 19, scale = 4)
    private BigDecimal amountAfter;

    @Column(name = "delegated_from", length = 180)
    private String delegatedFrom;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist
    void prePersist() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
