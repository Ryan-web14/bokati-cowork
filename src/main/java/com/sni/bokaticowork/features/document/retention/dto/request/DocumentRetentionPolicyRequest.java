package com.sni.bokaticowork.features.document.retention.dto.request;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.retention.enums.DocumentRetentionAction;
import com.sni.bokaticowork.features.document.retention.enums.DocumentRetentionReference;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentRetentionPolicyRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    private DocumentSpace space;

    private String documentTypeCode;

    @NotNull
    @Min(1)
    private Integer retentionDays;

    private DocumentRetentionReference retentionReference = DocumentRetentionReference.UPLOAD_DATE;

    private DocumentRetentionAction action = DocumentRetentionAction.ARCHIVE;
}
