package com.sni.bokaticowork.features.document.documentMaster.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDocumentSignatureRequest(
        @NotBlank String signerType,
        @NotNull Long signerId,
        @NotBlank String signerName,
        @Email String signerEmail
) {
}
