package com.sni.bokaticowork.features.booking.dto.response;

import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;

public record BookingAudiencePolicyResponse(
        String policyNumber,
        SubscriberType audienceType,
        String resourceTypeCode,
        String resourceGroupCode,
        Boolean approvalRequired,
        Integer maxActiveBookings,
        Integer maxBookingsPerDay,
        Integer maxBookingsPerWeek,
        Integer maxBookingsPerMonth,
        Integer maxNoShowsPerMonth,
        Integer minBookingNoticeMinutes,
        Integer maxBookingDurationMinutes,
        Boolean active
) {
}
