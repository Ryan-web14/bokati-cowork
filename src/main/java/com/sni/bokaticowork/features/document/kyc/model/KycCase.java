package com.sni.bokaticowork.features.document.kyc.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.KycRiskLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "kyc_case", uniqueConstraints = {
        @UniqueConstraint(name = "uk_kyc_case_code", columnNames = "code")
})
public class KycCase {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "owner_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentOwnerType ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private KycCaseStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "decision_comment")
    private String decisionComment;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "sla_deadline")
    private Instant slaDeadline;

    @Column(name = "last_reminder_sent_at")
    private Instant lastReminderSentAt;

    @Builder.Default
    @Column(name = "reminder_count")
    private Integer reminderCount = 0;

    @Builder.Default
    @Column(name = "risk_level", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private KycRiskLevel riskLevel = KycRiskLevel.LOW;

    @Builder.Default
    @Column(name = "kyc_level", nullable = false)
    private Integer kycLevel = 1;

    @PrePersist
    public void prePersist() {
        startedAt = startedAt == null ? Instant.now() : startedAt;
        status = status == null ? KycCaseStatus.NOT_STARTED : status;
        reminderCount = reminderCount == null ? 0 : reminderCount;
        riskLevel = riskLevel == null ? KycRiskLevel.LOW : riskLevel;
        kycLevel = kycLevel == null ? 1 : kycLevel;
    }
}
