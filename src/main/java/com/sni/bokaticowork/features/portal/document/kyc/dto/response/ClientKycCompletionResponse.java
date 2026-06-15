package com.sni.bokaticowork.features.portal.document.kyc.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientKycCompletionResponse {
    private String caseCode;
    private String caseStatus;
    private int completionPercent;
    private int totalRequired;
    private int totalSubmitted;
    private int totalVerified;
    private List<String> missingDocumentTypeCodes;
    private List<ClientKycRequirementResponse> requirements;
}
