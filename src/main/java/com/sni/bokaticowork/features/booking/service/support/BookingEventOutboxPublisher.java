package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.model.BookingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingEventOutboxPublisher {

    private static final String AGGREGATE_TYPE = "BOOKING";

    private final OutboxService outboxService;

    public void publish(BookingEvent event, boolean emailRequested) {
        if (event == null || event.getEventType() == null || isEmailAuditEvent(event.getEventType())) {
            return;
        }

        Booking booking = event.getBooking();
        if (booking == null || booking.getBookingNumber() == null || booking.getBookingNumber().isBlank()) {
            throw new IllegalStateException("Booking event cannot be published without a booking number");
        }

        BookingEventOutboxPayload payload = new BookingEventOutboxPayload(
                event.getEventNumber(),
                booking.getBookingNumber(),
                event.getEventType().name(),
                event.getTitle(),
                event.getDescription(),
                event.getPayloadJson(),
                emailRequested
        );

        outboxService.publish(event.getEventType().name(), AGGREGATE_TYPE, booking.getBookingNumber(), payload);
    }

    private boolean isEmailAuditEvent(BookingEventType eventType) {
        return eventType == BookingEventType.EMAIL_QUEUED
                || eventType == BookingEventType.EMAIL_SENT
                || eventType == BookingEventType.EMAIL_FAILED;
    }
}
