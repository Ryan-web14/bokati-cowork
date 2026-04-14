package com.sni.bokaticowork.features.contract.dto.request;

import com.sni.bokaticowork.features.contract.enums.ContractRenewalType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateContractRequest {
    @NotBlank
    private String title;
    private String description;
    @NotBlank
    private String templateCode;
    @NotNull
    private DocumentOwnerType ownerType;
    @NotBlank
    private String ownerCode;
    private String businessCode;
    private ContractRenewalType renewalType;
    private LocalDate effectiveDate;
    private LocalDate startDate;
    private LocalDate endDate;
    @NotNull
    private Long createdBy;
    @Valid
    private List<ContractPartyRequest> parties;
}
