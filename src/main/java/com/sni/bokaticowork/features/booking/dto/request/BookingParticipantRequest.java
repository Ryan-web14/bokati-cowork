package com.sni.bokaticowork.features.booking.dto.request;

import com.sni.bokaticowork.features.booking.enums.BookingParticipantRole;

public record BookingParticipantRequest(
        String memberCode,
        String name,
        String email,
        String phone,
        BookingParticipantRole role
) {
}
