package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CashRegisterStatisticsResponse(
        String registerCode,
        String registerName,
        Long sessionCount,
        Long movementCount,
        BigDecimal averageSessionDurationMinutes,
        BigDecimal averageVarianceAmount,
        List<HourlyVolume> peakHours,
        BigDecimal periodOverPeriodChangePercentage
) {
    public record HourlyVolume(int hourOfDay, long movementCount, BigDecimal totalAmount) {
    }
}
