package com.sni.bokaticowork.features.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingTrendResponse(
        LocalDate date,
        long bookingCount,
        long confirmedCount,
        long cancelledCount,
        BigDecimal revenue
) {
}
