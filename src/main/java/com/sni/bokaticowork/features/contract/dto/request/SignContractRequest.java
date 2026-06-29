package com.sni.bokaticowork.features.contract.dto.request;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

@Data
public class SignContractRequest {

    @AssertTrue(message = "Vous devez accepter les termes du contrat pour signer")
    private boolean accepted;

    private String consentText;
}
