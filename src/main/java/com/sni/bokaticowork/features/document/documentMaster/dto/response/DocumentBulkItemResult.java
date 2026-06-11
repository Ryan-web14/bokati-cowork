package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentBulkItemResult {
    private String code;
    private String status;
    private String reason;
}
