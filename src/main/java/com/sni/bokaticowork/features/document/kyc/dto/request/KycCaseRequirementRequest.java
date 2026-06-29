package com.sni.bokaticowork.features.document.kyc.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KycCaseRequirementRequest {

    @NotBlank
    private String documentTypeCode;

    private String documentTypeName;

    private Boolean required;
}
