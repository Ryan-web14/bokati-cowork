package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DocumentBulkActionResponse {
    private int total;
    private int succeeded;
    private int failed;
    private List<DocumentBulkItemResult> results;
}
