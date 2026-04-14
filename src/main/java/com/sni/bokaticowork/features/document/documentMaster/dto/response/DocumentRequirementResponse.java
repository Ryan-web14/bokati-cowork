package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentRequirementResponse {
    private Long id;
    private DocumentOwnerType ownerType;
    private String documentTypeCode;
    private String documentTypeName;
    private String customerType;
    private String businessLegalForm;
    private Boolean required;
    private Boolean active;
}
