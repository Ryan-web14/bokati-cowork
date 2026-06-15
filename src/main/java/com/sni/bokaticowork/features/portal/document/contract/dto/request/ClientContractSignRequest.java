package com.sni.bokaticowork.features.portal.document.contract.dto.request;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

@Data
public class ClientContractSignRequest {

    @AssertTrue(message = "You must accept the contract terms to sign")
    private boolean accepted;

    private String signatureData;
}
