package com.sni.bokaticowork.features.document.kyc.dto.request;

import lombok.Data;

@Data
public class KycAssignRequest {
    private Long assignedTo;
    private String email;
}
