package com.sni.bokaticowork.features.document.kyc.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KycDecisionRequest {

    @NotNull
    private Long reviewedBy;

    private String comment;
}
