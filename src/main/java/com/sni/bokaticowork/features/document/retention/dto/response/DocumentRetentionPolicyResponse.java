package com.sni.bokaticowork.features.document.retention.dto.response;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.retention.enums.DocumentRetentionAction;
import com.sni.bokaticowork.features.document.retention.enums.DocumentRetentionReference;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DocumentRetentionPolicyResponse {

    private Long id;
    private String code;
    private String name;
    private DocumentSpace space;
    private String documentTypeCode;
    private Integer retentionDays;
    private DocumentRetentionReference retentionReference;
    private DocumentRetentionAction action;
    private Boolean active;
    private Instant createdAt;
    private Long createdBy;
}
