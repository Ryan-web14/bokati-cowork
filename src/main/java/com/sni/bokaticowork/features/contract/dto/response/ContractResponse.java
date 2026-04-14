package com.sni.bokaticowork.features.contract.dto.response;

import com.sni.bokaticowork.features.contract.enums.ContractRenewalType;
import com.sni.bokaticowork.features.contract.enums.ContractStatus;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ContractResponse {
    private String contractCode;
    private String title;
    private String description;
    private String templateCode;
    private DocumentOwnerType ownerType;
    private String ownerCode;
    private String businessCode;
    private ContractStatus status;
    private ContractRenewalType renewalType;
    private LocalDate effectiveDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private Instant signedAt;
    private Instant activatedAt;
    private Instant terminatedAt;
    private String terminationReason;
    private String draftDocumentCode;
    private String signedDocumentCode;
    private Long createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ContractPartyResponse> parties;
}
