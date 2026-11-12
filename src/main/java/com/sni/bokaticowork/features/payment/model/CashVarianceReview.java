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

/** Un ecart a decider : ouverture, cloture, comptage inopine, passation, cloture d'office. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_variance_review")
public class CashVarianceReview {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "review_number", nullable = false, unique = true, length = 100)
    private String reviewNumber;

    @Column(name = "register_code", nullable = false, length = 100)
    private String registerCode;

    @Column(name = "session_number", length = 100)
    private String sessionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 30)
    private com.sni.bokaticowork.features.payment.enums.CashVarianceKind kind;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "count_number", length = 100)
    private String countNumber;

    @Column(name = "cashier", length = 180)
    private String cashier;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private com.sni.bokaticowork.features.payment.enums.CashVarianceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 30)
    private com.sni.bokaticowork.features.payment.enums.CashVarianceDecision decision;

    @Column(name = "decided_by", length = 180)
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_note", length = 1000)
    private String decisionNote;

    @Column(name = "contest_deadline")
    private Instant contestDeadline;

    @Column(name = "contested_at")
    private Instant contestedAt;

    @Column(name = "contest_reason", length = 1000)
    private String contestReason;

    @Column(name = "review_due_at")
    private Instant reviewDueAt;

    @Column(name = "entry_number", length = 100)
    private String entryNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
