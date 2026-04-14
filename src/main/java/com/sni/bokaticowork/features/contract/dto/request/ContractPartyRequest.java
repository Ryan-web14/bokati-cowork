package com.sni.bokaticowork.features.contract.dto.request;

import com.sni.bokaticowork.features.contract.enums.ContractPartyRole;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ContractPartyRequest {
    @NotNull
    private DocumentOwnerType partyType;
    @NotBlank
    private String partyCode;
    @NotBlank
    private String displayName;
    private String email;
    private String phone;
    @NotNull
    private ContractPartyRole role;
    private Integer signOrder;
    private Boolean mustSign;
}
