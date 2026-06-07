package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record CashSessionStatisticsResponse(
        String sessionNumber,
        String registerCode,
        String openedBy,
        Instant openedAt,
        Instant closedAt,
        Long durationMinutes,
        Long movementCount,
        BigDecimal transactionsPerHour,
        BigDecimal totalInflow,
        BigDecimal totalOutflow,
        BigDecimal inOutRatio,
        Long idleMinutes,
        BigDecimal averageMovementAmount,
        BigDecimal varianceAmount,
        BigDecimal variancePercentage
) {
}
