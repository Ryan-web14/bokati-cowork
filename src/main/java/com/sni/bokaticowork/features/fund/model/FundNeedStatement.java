package com.sni.bokaticowork.features.fund.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Etat de besoin (lot F5). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "fund_need_statement")
public class FundNeedStatement {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "number", nullable = false)
    private String number;

    @Column(name = "requester", nullable = false)
    private String requester;

    @Column(name = "department")
    private String department;

    @Column(name = "location_code")
    private String locationCode;

    @Column(name = "cost_center")
    private String costCenter;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "urgency", nullable = false)
    private com.sni.bokaticowork.features.fund.enums.FundUrgency urgency = com.sni.bokaticowork.features.fund.enums.FundUrgency.NORMAL;

    @Column(name = "wanted_date")
    private java.time.LocalDate wantedDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false)
    private com.sni.bokaticowork.features.fund.enums.FundNeedStatus status = com.sni.bokaticowork.features.fund.enums.FundNeedStatus.DRAFT;

    @Builder.Default
    @Column(name = "total_amount", nullable = false)
    private java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;

    @Column(name = "submitted_at")
    private java.time.Instant submittedAt;

    @Column(name = "decided_by")
    private String decidedBy;

    @Column(name = "decided_at")
    private java.time.Instant decidedAt;

    @Column(name = "decision_note")
    private String decisionNote;

    @Column(name = "attachments", columnDefinition = "TEXT")
    private String attachments;

    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private java.time.Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
        updatedAt = java.time.Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = java.time.Instant.now();
    }
}
