package com.sni.bokaticowork.features.contract.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateContractStatusRequest {
    @NotBlank
    private String status;
    private String reason;
}
