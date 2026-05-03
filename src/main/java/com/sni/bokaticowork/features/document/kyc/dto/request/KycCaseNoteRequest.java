package com.sni.bokaticowork.features.document.kyc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KycCaseNoteRequest {
    @NotBlank
    private String content;

    @NotNull
    private Long authorId;

    private Boolean internal = Boolean.TRUE;
}
