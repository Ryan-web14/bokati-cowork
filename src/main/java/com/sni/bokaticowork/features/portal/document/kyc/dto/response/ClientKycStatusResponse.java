package com.sni.bokaticowork.features.portal.document.kyc.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientKycStatusResponse {
    private String caseCode;
    private String status;
    private String riskLevel;
    private int kycLevel;
    private int completionPercent;
    private boolean complete;
    private boolean approved;
    private Instant startedAt;
    private Instant submittedAt;
    private Instant completedAt;
    private String decisionComment;
    private List<String> missingDocumentTypeCodes;
    private List<ClientKycRequirementResponse> requirements;
    private List<ClientKycDocumentResponse> documents;
}
