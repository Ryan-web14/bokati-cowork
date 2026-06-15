package com.sni.bokaticowork.features.portal.document.kyc.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientKycRequirementResponse {
    private String documentTypeCode;
    private String documentTypeName;
    private boolean required;
    private String status;
    private String documentCode;
}
