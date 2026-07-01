package com.sni.bokaticowork.features.document.documentMaster.dto.request;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentCategory;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentTypeRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    @NotNull
    private DocumentCategory category;

    private DocumentOwnerType ownerType;

    private String description;

    private String helpText;

    private String documentDetails;

    private Boolean required;

    private Boolean requiresExpiryDate;

    private Boolean requiresIssueDate;

    private Boolean requiresDocumentNumber;

    private Boolean requiresReview;

    private Boolean autoApprove;

    private Integer autoApproveAfterDays;

    private Boolean requiresSignature;

    private Boolean multipleAllowed;

    private Boolean requiresBackSide;

    private String allowedMimeTypes;

    private Long maxFileSizeBytes;

    private Boolean active;
}
