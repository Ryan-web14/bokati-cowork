package com.sni.bokaticowork.features.document.documentMaster.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DocumentBulkApproveRequest {

    @NotEmpty
    private List<String> codes;

    /** Ignore pour un compte connecte : l\'auteur est toujours le compte connecte (lot KG0). */
    private Long reviewedBy;

    private String comment;
}
