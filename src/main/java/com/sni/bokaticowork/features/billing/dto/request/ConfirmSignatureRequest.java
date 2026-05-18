package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Confirmation de signature soumise depuis la page publique.
 */
public record ConfirmSignatureRequest(
        @NotBlank String signerName,
        String signerEmail,
        @NotBlank String signatureImageBase64,
        String ipAddress,
        String userAgent
) {
}
