package com.sni.bokaticowork.features.contract.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class RequestContractSigningRequest {

    /**
     * Pour une organisation, facultatifs : le premier signataire designe (representant legal ou
     * membre autorise a signer) est retenu. Donnes, ils doivent designer l'un d'eux. Pour un
     * particulier, requis.
     */
    @Email
    private String signerEmail;

    private String signerName;
}
