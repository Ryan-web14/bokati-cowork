package com.sni.bokaticowork.features.document.kyc.dto.response;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class KycCaseResponse {
    private String code;
    private String ownerName;
    private DocumentOwnerType ownerType;
    private Long ownerId;
    private KycCaseStatus status;
    private Instant startedAt;
    private Instant submittedAt;
    private Instant completedAt;
    private Long reviewedBy;
    private Instant reviewedAt;
    private String decisionComment;
    private boolean complete;
    private boolean approved;
    private List<String> missingDocumentTypeCodes;
    private List<KycDocumentResponse> documents;
}
