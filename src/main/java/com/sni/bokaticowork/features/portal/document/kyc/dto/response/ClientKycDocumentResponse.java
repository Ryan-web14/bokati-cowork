package com.sni.bokaticowork.features.portal.document.kyc.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientKycDocumentResponse {
    private Long id;
    private String documentCode;
    private String documentType;
    private String documentTypeName;
    private String documentNumber;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private String status;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private Instant uploadedAt;
}
