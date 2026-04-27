package com.sni.bokaticowork.features.booking.dto.request;

import jakarta.validation.constraints.Email;

import java.time.Instant;

public record CreateBookingQuotaOverrideRequest(
        String memberId,
        String customerId,
        String businessCode,
        @Email String email,
        String phone,
        String resourceCode,
        Integer extraActiveBookings,
        Integer extraBookingsPerDay,
        Integer extraBookingsPerWeek,
        Integer extraBookingsPerMonth,
        String reason,
        String approvedBy,
        Instant validFrom,
        Instant validUntil
) {
        public BookingIdentityLookup identityLookup() {
                return new BookingIdentityLookup(memberId, customerId, businessCode, email, phone, Boolean.FALSE, null, null, null);
        }
}
