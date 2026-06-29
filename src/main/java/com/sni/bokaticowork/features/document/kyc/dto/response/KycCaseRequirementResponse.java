package com.sni.bokaticowork.features.document.kyc.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KycCaseRequirementResponse {
    private Long id;
    private String kycCaseCode;
    private String documentTypeCode;
    private String documentTypeName;
    private Boolean required;
    private Boolean requiresBackSide;
    private Boolean active;
}
