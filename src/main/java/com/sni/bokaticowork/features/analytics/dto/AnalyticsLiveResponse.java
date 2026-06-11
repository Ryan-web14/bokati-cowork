package com.sni.bokaticowork.features.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AnalyticsLiveResponse(
        Instant generatedAt,

        // ── Instantané (maintenant) ──────────────────────────────────
        long activeBookings,
        long checkedInBookings,
        long occupiedResources,
        long totalBookableResources,
        BigDecimal occupancyRate,
        long availableResources,
        long pendingApproval,
        long upcomingNextHour,
        long activeHolds,

        // ── Aujourd'hui ──────────────────────────────────────────────
        long bookingsToday,
        BigDecimal revenueToday,
        BigDecimal avgBookingAmount,
        long bookedMinutesToday,
        long cancelledToday,
        long noShowToday,
        long checkInsToday,
        long newMembersToday
) {
}
