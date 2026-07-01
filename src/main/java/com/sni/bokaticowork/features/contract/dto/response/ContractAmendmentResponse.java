package com.sni.bokaticowork.features.contract.dto.response;

import com.sni.bokaticowork.features.contract.enums.AmendmentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ContractAmendmentResponse {
    private String code;
    private String originalContractCode;
    private AmendmentStatus status;
    private String description;
    private Long proposedBy;
    private Instant proposedAt;
    private LocalDate effectiveDate;
    private String draftDocumentCode;
    private String signedDocumentCode;
    private Instant signedAt;
    private Instant activatedAt;
    private Long reviewedBy;
    private String reviewComment;
    private String rejectionReason;
    private String cancellationReason;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ContractAmendmentSectionResponse> sections;
    private List<ContractAmendmentVariableResponse> variableChanges;
}
