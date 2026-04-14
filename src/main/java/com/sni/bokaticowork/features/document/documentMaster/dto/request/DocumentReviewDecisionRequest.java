package com.sni.bokaticowork.features.document.documentMaster.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentReviewDecisionRequest {

    @NotNull
    private Long reviewedBy;

    private String comment;

    private String rejectionReasonCode;

    private String rejectionReasonDetail;
}
