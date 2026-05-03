package com.sni.bokaticowork.features.document.kyc.dto.request;

import com.sni.bokaticowork.features.document.kyc.KycRiskLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KycRiskLevelRequest {
    @NotNull
    private KycRiskLevel riskLevel;

    private Long reviewedBy;

    private String comment;
}
