package com.sni.bokaticowork.features.booking.dto.request;

public record BookingCheckRequest(
        String actor,
        String note,
        Boolean sendEmail
) {
}
