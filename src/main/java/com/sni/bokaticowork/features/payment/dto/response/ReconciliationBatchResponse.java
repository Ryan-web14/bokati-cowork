package com.sni.bokaticowork.features.payment.dto.response;

import com.sni.bokaticowork.features.payment.enums.ReconciliationStatus;

import java.time.Instant;

public record ReconciliationBatchResponse(
        String batchNumber,
        String provider,
        ReconciliationStatus status,
        String createdBy,
        Instant createdAt,
        Instant completedAt
) {
}
