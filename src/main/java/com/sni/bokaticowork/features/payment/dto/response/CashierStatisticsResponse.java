package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;

public record CashierStatisticsResponse(
        String cashierCode,
        Long sessionCount,
        Long closedSessionCount,
        Long reviewSessionCount,
        BigDecimal reviewFrequencyPercentage,
        BigDecimal averageVarianceAmount,
        BigDecimal varianceStandardDeviation,
        String varianceTrend,
        BigDecimal averageSessionDurationMinutes,
        BigDecimal riskScore
) {
}
