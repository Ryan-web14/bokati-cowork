package com.sni.bokaticowork.features.document.kyc.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class KycCaseBulkRejectRequest {

    @Valid
    @NotEmpty
    private List<KycBulkRejectItemRequest> items;

    @NotNull
    private Long reviewedBy;
}
