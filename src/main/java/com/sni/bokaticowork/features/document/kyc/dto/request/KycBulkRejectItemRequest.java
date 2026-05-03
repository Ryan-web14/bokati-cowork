package com.sni.bokaticowork.features.document.kyc.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KycBulkRejectItemRequest {
    @NotBlank
    private String documentCode;

    @NotBlank
    private String reason;
}
