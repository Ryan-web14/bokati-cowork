package com.sni.bokaticowork.features.document.kyc.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class KycDocumentOcrResultResponse {
    private String documentCode;
    private String extractedFirstName;
    private String extractedLastName;
    private LocalDate extractedDateOfBirth;
    private LocalDate extractedExpiryDate;
    private String extractedDocumentNumber;
    private String extractedNationality;
    private BigDecimal confidenceScore;
    private String rawOcrJson;
    private Instant processedAt;
}
