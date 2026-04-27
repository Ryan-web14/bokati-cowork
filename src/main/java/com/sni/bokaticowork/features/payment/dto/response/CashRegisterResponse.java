package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record CashRegisterResponse(
        String registerCode,
        String name,
        String locationCode,
        String businessEntityCode,
        String deviceCode,
        Boolean active,
        Boolean cashControlEnabled,
        BigDecimal maxCashAmount,
        Instant createdAt,
        Instant updatedAt
) {
}
