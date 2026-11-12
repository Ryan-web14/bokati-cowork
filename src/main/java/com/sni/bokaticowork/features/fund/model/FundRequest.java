package com.sni.bokaticowork.features.fund.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Demande de fonds (lot F5) et suivi de l'avance (lot F6). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "fund_request")
public class FundRequest {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "number", nullable = false)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    private com.sni.bokaticowork.features.fund.enums.FundRequestType requestType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false)
    private com.sni.bokaticowork.features.fund.enums.FundRequestStatus status = com.sni.bokaticowork.features.fund.enums.FundRequestStatus.DRAFT;

    @Column(name = "requester", nullable = false)
    private String requester;

    @Column(name = "requester_department")
    private String requesterDepartment;

    @Column(name = "requester_phone")
    private String requesterPhone;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "beneficiary_type", nullable = false)
    private com.sni.bokaticowork.features.fund.enums.FundBeneficiaryType beneficiaryType = com.sni.bokaticowork.features.fund.enums.FundBeneficiaryType.SELF;

    @Column(name = "beneficiary_code")
    private String beneficiaryCode;

    @Column(name = "beneficiary_name")
    private String beneficiaryName;

    @Column(name = "beneficiary_id_number")
    private String beneficiaryIdNumber;

    @Column(name = "beneficiary_phone")
    private String beneficiaryPhone;

    @Column(name = "register_code")
    private String registerCode;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Builder.Default
    @Column(name = "total_amount", nullable = false)
    private java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;

    @Column(name = "approved_amount")
    private java.math.BigDecimal approvedAmount;

    @Builder.Default
    @Column(name = "disbursed_amount", nullable = false)
    private java.math.BigDecimal disbursedAmount = java.math.BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "justified_amount", nullable = false)
    private java.math.BigDecimal justifiedAmount = java.math.BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "returned_amount", nullable = false)
    private java.math.BigDecimal returnedAmount = java.math.BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "complement_amount", nullable = false)
    private java.math.BigDecimal complementAmount = java.math.BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "delivery_mode", nullable = false)
    private com.sni.bokaticowork.features.fund.enums.FundDeliveryMode deliveryMode = com.sni.bokaticowork.features.fund.enums.FundDeliveryMode.CASH;

    @Column(name = "wanted_date")
    private java.time.LocalDate wantedDate;

    @Column(name = "mission_destination")
    private String missionDestination;

    @Column(name = "mission_start")
    private java.time.LocalDate missionStart;

    @Column(name = "mission_end")
    private java.time.LocalDate missionEnd;

    @Builder.Default
    @Column(name = "unplanned", nullable = false)
    private Boolean unplanned = Boolean.FALSE;

    @Column(name = "unplanned_reason")
    private String unplannedReason;

    @Column(name = "need_statement_number")
    private String needStatementNumber;

    @Column(name = "transfer_order_number")
    private String transferOrderNumber;

    @Builder.Default
    @Column(name = "required_levels", nullable = false)
    private Integer requiredLevels = 1;

    @Builder.Default
    @Column(name = "current_level", nullable = false)
    private Integer currentLevel = 0;

    @Column(name = "last_comment")
    private String lastComment;

    @Column(name = "submitted_at")
    private java.time.Instant submittedAt;

    @Column(name = "level_since")
    private java.time.Instant levelSince;

    @Column(name = "approver_reminded_at")
    private java.time.Instant approverRemindedAt;

    @Column(name = "approved_at")
    private java.time.Instant approvedAt;

    @Column(name = "expires_at")
    private java.time.Instant expiresAt;

    @Column(name = "disbursed_at")
    private java.time.Instant disbursedAt;

    @Column(name = "justification_due_date")
    private java.time.LocalDate justificationDueDate;

    @Builder.Default
    @Column(name = "justification_reminders", nullable = false)
    private Integer justificationReminders = 0;

    @Column(name = "justification_reminded_at")
    private java.time.Instant justificationRemindedAt;

    @Column(name = "settled_at")
    private java.time.Instant settledAt;

    @Column(name = "cancelled_at")
    private java.time.Instant cancelledAt;

    /** Validation budgetaire supplementaire (lot F16) : OVERRIDE (depassement bloquant) ou UNPLANNED. */
    @Column(name = "budget_approval")
    private String budgetApproval;

    @Column(name = "forecast_number")
    private String forecastNumber;

    @Column(name = "unplanned_code")
    private String unplannedCode;

    @Column(name = "legacy_request_number")
    private String legacyRequestNumber;

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
