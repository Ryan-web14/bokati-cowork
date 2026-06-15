package com.sni.bokaticowork.features.portal.document.contract.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientContractResponse {
    private String contractCode;
    private String title;
    private String description;
    private String status;
    private String renewalType;
    private LocalDate effectiveDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private Instant signedAt;
    private Instant activatedAt;
    private Instant terminatedAt;
    private String terminationReason;
    private String draftDocumentCode;
    private String signedDocumentCode;
    private boolean canSign;
    private boolean canDownloadPdf;
    private Instant createdAt;
    private Instant updatedAt;
}
