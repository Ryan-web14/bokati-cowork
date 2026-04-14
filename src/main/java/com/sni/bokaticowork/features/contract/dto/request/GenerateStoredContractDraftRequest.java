package com.sni.bokaticowork.features.contract.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerateStoredContractDraftRequest {
    @NotNull
    private Long uploadedBy;
}
