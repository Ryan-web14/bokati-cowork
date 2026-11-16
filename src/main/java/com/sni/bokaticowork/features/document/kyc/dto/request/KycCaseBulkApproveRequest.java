package com.sni.bokaticowork.features.document.kyc.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class KycCaseBulkApproveRequest {

    @NotEmpty
    private List<String> documentCodes;

    /** Ignore pour un compte connecte : l\'auteur est toujours le compte connecte (lot KG0). */
    private Long reviewedBy;
}
