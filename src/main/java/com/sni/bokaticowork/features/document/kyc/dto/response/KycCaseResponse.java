package com.sni.bokaticowork.features.document.kyc.dto.response;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.KycRiskLevel;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class KycCaseResponse {
    private String code;
    private String ownerName;
    private String ownerCode;
    private DocumentOwnerType ownerType;
    private Long ownerId;
    private KycCaseStatus status;
    private Instant startedAt;
    private Instant submittedAt;
    private Instant completedAt;
    private Long reviewedBy;
    private String reviewedByEmail;
    private Instant reviewedAt;
    private String decisionComment;
    /** Lot KG7 : motif normalise de la derniere decision et son libelle. */
    private String decisionReasonCode;
    private String decisionReasonLabel;
    private Long assignedTo;
    private String assignedToEmail;
    private Instant assignedAt;
    private Instant slaDeadline;
    private Instant lastReminderSentAt;
    private Integer reminderCount;
    private KycRiskLevel riskLevel;
    private Integer kycLevel;
    /** Niveau demande (les pieces de ce niveau sont exigees) ; {@code kycLevel} est le niveau atteint. */
    private Integer requestedKycLevel;
    private boolean complete;
    private boolean approved;
    private List<String> missingDocumentTypeCodes;
    private List<KycRequirementStatus> requirements;
    private List<KycDocumentResponse> documents;
}
