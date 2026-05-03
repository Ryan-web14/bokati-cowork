package com.sni.bokaticowork.features.document.kyc.dto.response;

import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KycRequirementStatus {
    private String documentTypeCode;
    private String documentTypeName;
    private boolean required;
    private KycDocumentVerificationStatus status;
    private String documentCode;
}
