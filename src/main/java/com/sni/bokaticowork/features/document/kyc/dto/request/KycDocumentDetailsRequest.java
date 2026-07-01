package com.sni.bokaticowork.features.document.kyc.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class KycDocumentDetailsRequest {
    private String documentNumber;
    private LocalDate issueDate;
    private LocalDate expiryDate;
}
