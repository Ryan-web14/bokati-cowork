package com.sni.bokaticowork.features.document.documentMaster.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DocumentBulkCorrectionRequest {

    @NotEmpty
    private List<String> codes;

    @NotNull
    private Long reviewedBy;

    @NotBlank
    private String correctionNote;

    @Min(1)
    private Integer deadlineDays = 7;
}
