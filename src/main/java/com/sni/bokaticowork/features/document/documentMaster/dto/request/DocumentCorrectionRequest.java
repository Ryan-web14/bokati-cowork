package com.sni.bokaticowork.features.document.documentMaster.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentCorrectionRequest {

    @NotNull
    private Long reviewedBy;

    @NotBlank
    private String correctionNote;

    private String comment;

    @Min(1)
    private Integer deadlineDays = 7;
}
