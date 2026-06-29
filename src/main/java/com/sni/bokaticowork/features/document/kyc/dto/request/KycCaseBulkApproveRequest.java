package com.sni.bokaticowork.features.document.kyc.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class KycCaseBulkApproveRequest {

    @NotEmpty
    private List<String> documentCodes;

    @NotNull
    private Long reviewedBy;
}
