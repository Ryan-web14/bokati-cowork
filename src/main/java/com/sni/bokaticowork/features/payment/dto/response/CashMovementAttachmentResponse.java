package com.sni.bokaticowork.features.payment.dto.response;

import java.time.Instant;

public record CashMovementAttachmentResponse(
        Long id,
        String fileName,
        String contentType,
        String storagePath,
        String label,
        String uploadedBy,
        Instant uploadedAt
) {
}
