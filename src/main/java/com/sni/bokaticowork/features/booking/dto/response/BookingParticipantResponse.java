package com.sni.bokaticowork.features.booking.dto.response;

import com.sni.bokaticowork.features.booking.enums.BookingParticipantRole;
import com.sni.bokaticowork.features.booking.enums.BookingParticipantStatus;

public record BookingParticipantResponse(
        Long id,
        String memberCode,
        String name,
        String email,
        String phone,
        BookingParticipantRole role,
        BookingParticipantStatus status,
        /** Lot B9d : invitation envoyee, reponse, arrivee pointee. */
        java.time.Instant invitedAt,
        java.time.Instant respondedAt,
        java.time.Instant checkedInAt
) {
}
