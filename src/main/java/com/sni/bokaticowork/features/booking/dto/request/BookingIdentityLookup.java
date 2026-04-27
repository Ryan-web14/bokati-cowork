package com.sni.bokaticowork.features.booking.dto.request;

public record BookingIdentityLookup(
        String memberId,
        String customerId,
        String businessCode,
        String email,
        String phone,
        Boolean walkIn,
        String contactName,
        String contactEmail,
        String contactPhone
) {
}
