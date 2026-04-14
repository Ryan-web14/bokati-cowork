package com.sni.bokaticowork.features.contract.dto.response;

import com.sni.bokaticowork.features.contract.enums.ContractPartyRole;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContractPartyResponse {
    private DocumentOwnerType partyType;
    private String partyCode;
    private String displayName;
    private String email;
    private String phone;
    private ContractPartyRole role;
    private Integer signOrder;
    private Boolean mustSign;
}
