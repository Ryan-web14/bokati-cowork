package com.sni.bokaticowork.features.document.kyc.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KycCaseCorrectionRequest {

    @NotBlank
    private String documentCode;

    @NotNull
    private Long reviewedBy;

    @NotBlank
    private String correctionNote;

    @Min(1)
    private Integer deadlineDays = 7;
}
