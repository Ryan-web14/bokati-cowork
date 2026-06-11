package com.sni.bokaticowork.features.payment.dto.response;

import com.sni.bokaticowork.features.payment.enums.CashAnomalySeverity;
import com.sni.bokaticowork.features.payment.enums.CashAnomalyStatus;
import com.sni.bokaticowork.features.payment.enums.CashAnomalyType;

import java.math.BigDecimal;
import java.time.Instant;

public record CashAnomalyFlagResponse(
        String flagNumber,
        String sessionNumber,
        String movementNumber,
        String registerCode,
        CashAnomalyType anomalyType,
        CashAnomalySeverity severity,
        BigDecimal score,
        String description,
        Instant detectedAt,
        CashAnomalyStatus status,
        String reviewedBy,
        Instant reviewedAt,
        String reviewNote
) {
}
