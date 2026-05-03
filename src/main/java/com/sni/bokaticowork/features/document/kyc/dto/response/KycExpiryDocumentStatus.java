package com.sni.bokaticowork.features.document.kyc.dto.response;

import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class KycExpiryDocumentStatus {
    private String documentCode;
    private String documentType;
    private LocalDate expiryDate;
    private Long daysUntilExpiry;
    private Boolean expired;
    private KycDocumentVerificationStatus status;
}
