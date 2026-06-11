package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddCashMovementAttachmentRequest(
        @NotBlank String fileName,
        String contentType,
        @NotBlank String storagePath,
        String label,
        String uploadedBy
) {
}
