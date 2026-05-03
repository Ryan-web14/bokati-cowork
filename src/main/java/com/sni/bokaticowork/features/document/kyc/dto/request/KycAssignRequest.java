package com.sni.bokaticowork.features.document.kyc.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KycAssignRequest {
    @NotNull
    private Long assignedTo;
}
