package com.sni.bokaticowork.features.contract.dto.request;

import com.sni.bokaticowork.features.contract.enums.ContractRenewalType;
import jakarta.validation.Valid;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateContractRequest {
    private String title;
    private String description;
    private String templateCode;
    private String businessCode;
    private ContractRenewalType renewalType;
    private LocalDate effectiveDate;
    private LocalDate startDate;
    private LocalDate endDate;
    @Valid
    private List<ContractPartyRequest> parties;
}
