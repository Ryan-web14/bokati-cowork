package com.sni.bokaticowork.features.document.kyc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycCrossValidationRuleResponse {
    private Long id;
    private String documentTypeCode1;
    private String documentTypeCode2;
    private String fieldToCompare;
    private Boolean blocking;
    private Boolean active;
}
