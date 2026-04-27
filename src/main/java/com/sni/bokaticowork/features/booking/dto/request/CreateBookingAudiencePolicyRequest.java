package com.sni.bokaticowork.features.booking.dto.request;

import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.validation.constraints.NotNull;

public record CreateBookingAudiencePolicyRequest(
        @NotNull SubscriberType audienceType,
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
