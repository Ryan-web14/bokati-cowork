package com.sni.bokaticowork.features.booking.dto.response;

import com.sni.bokaticowork.features.booking.enums.BookingEventType;

import java.time.Instant;

public record BookingEventResponse(
        String eventNumber,
        String bookingNumber,
        BookingEventType eventType,
        String title,
        String description,
        String payloadJson,
        Instant createdAt
) {
}
