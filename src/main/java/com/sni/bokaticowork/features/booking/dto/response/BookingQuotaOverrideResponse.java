package com.sni.bokaticowork.features.booking.dto.response;

import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;

import java.time.Instant;

public record BookingQuotaOverrideResponse(
        String overrideNumber,
        SubscriberType ownerType,
        String ownerCode,
        String resourceCode,
        Integer extraActiveBookings,
        Integer extraBookingsPerDay,
        Integer extraBookingsPerWeek,
        Integer extraBookingsPerMonth,
        String reason,
        String approvedBy,
        Instant validFrom,
        Instant validUntil,
        Boolean active
) {
}
