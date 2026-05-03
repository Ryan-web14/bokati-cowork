package com.sni.bokaticowork.features.document.kyc.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KycBulkItemResult {
    private String documentCode;
    private Boolean success;
    private String error;
}
