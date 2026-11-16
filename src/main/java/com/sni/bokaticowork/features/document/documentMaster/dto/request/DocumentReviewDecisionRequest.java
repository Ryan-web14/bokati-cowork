package com.sni.bokaticowork.features.document.documentMaster.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentReviewDecisionRequest {

    /** Ignore pour un compte connecte : l\'auteur est toujours le compte connecte (lot KG0). */
    private Long reviewedBy;

    private String comment;

    private String rejectionReasonCode;

    private String rejectionReasonDetail;
}
