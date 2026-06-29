package com.sni.bokaticowork.features.contract.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestContractSigningRequest {

    @NotBlank
    @Email
    private String signerEmail;

    @NotBlank
    private String signerName;
}
