package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentCategory;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentTypeResponse {
    private String code;
    private String name;
    private DocumentCategory category;
    private DocumentOwnerType ownerType;
    private String description;
    private String helpText;
    private String documentDetails;
    private Boolean required;
    private Boolean requiresExpiryDate;
    private Boolean requiresReview;
    private Boolean requiresSignature;
    private Boolean multipleAllowed;
    private String allowedMimeTypes;
    private Long maxFileSizeBytes;
    private Boolean active;
}
