package com.sni.bokaticowork.features.document.documentMaster.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DocumentBulkApproveRequest {

    @NotEmpty
    private List<String> codes;

    @NotNull
    private Long reviewedBy;

    private String comment;
}
