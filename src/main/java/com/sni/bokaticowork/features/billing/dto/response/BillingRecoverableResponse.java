package com.sni.bokaticowork.features.billing.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record BillingRecoverableResponse(
        String recoverableNumber,
        String documentNumber,
        String itemDescription,
        BigDecimal quantity,
        String unit,
        String sourceType,
        String sourceCode,
        String status,
        String notes,
        Instant recoveredAt,
        String recoveredBy,
        Instant createdAt
) {
}
