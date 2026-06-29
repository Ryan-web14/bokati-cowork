package com.sni.bokaticowork.features.booking.service.support;

public record BookingEventOutboxPayload(
        String bookingEventNumber,
        String bookingNumber,
        String eventType,
        String title,
        String description,
        String payloadJson,
        boolean emailRequested
) {
}
