package com.sni.bokaticowork.features.document.kyc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycCrossValidationRuleRequest {

    @NotBlank
    @Size(max = 150)
    private String documentTypeCode1;

    @NotBlank
    @Size(max = 150)
    private String documentTypeCode2;

    @NotBlank
    @Size(max = 80)
    private String fieldToCompare;

    private Boolean blocking;

    private Boolean active;
}
