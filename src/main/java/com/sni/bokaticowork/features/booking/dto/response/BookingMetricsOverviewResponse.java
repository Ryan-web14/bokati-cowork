package com.sni.bokaticowork.features.booking.dto.response;

import java.math.BigDecimal;

public record BookingMetricsOverviewResponse(
        long draftBookings,
        long pendingApprovalBookings,
        long confirmedBookings,
        long completedBookings,
        long cancelledBookings,
        long noShowBookings,
        long activeHolds,
        BigDecimal confirmedAmount,
        BigDecimal completedAmount
) {
}
