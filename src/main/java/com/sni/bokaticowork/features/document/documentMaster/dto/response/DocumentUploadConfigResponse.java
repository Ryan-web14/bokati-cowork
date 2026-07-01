package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DocumentUploadConfigResponse {
    private String documentTypeCode;
    private String documentTypeName;
    private String helpText;
    private boolean requiresDocumentNumber;
    private boolean requiresIssueDate;
    private boolean requiresExpiryDate;
    private boolean requiresBackSide;
    private List<String> allowedMimeTypes;
    private long maxFileSizeBytes;
    private String category;
}
