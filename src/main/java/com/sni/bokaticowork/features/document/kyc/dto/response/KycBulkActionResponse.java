package com.sni.bokaticowork.features.document.kyc.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class KycBulkActionResponse {
    private Integer processed;
    private Integer succeeded;
    private Integer failed;
    private List<KycBulkItemResult> results;
}
