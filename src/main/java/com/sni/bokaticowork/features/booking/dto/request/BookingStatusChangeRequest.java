package com.sni.bokaticowork.features.booking.dto.request;

public record BookingStatusChangeRequest(
        String changedBy,
        String reason,
        Boolean sendEmail
) {
}
