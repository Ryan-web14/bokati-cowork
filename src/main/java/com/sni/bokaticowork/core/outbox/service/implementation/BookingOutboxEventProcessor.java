package com.sni.bokaticowork.core.outbox.service.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxEventProcessor;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.support.BookingEmailNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingOutboxEventProcessor implements OutboxEventProcessor {

    private final ObjectMapper objectMapper;
    private final BookingRepository bookingRepository;
    private final ObjectProvider<BookingEmailNotifier> emailNotifierProvider;

    @Override
    public boolean supports(OutboxEvent event) {
        return "BOOKING".equalsIgnoreCase(event.getAggregateType());
    }

    @Override
    public void process(OutboxEvent event) {
        JsonNode payload = readPayload(event);
        BookingEventType eventType = resolveEventType(event, payload);

        if (eventType == null || !emailRequested(payload) || !isEmailable(eventType)) {
            log.debug("Booking outbox event {} processed without email dispatch", event.getId());
            return;
        }

        String bookingNumber = textValue(payload, "bookingNumber");
        if (!StringUtils.hasText(bookingNumber)) {
            bookingNumber = event.getAggregateId();
        }

        Booking booking = bookingRepository.findPublicByBookingNumberWithResource(bookingNumber)
                .orElse(null);
        if (booking == null) {
            log.warn("Booking not found for outbox event {} bookingNumber={} · skipping email", event.getId(), bookingNumber);
            return;
        }

        try {
            emailNotifierProvider.getObject().notify(booking, eventType);
            log.info("Booking email notification queued for booking={} event={}", bookingNumber, eventType);
        } catch (Exception ex) {
            log.warn("Failed to send booking email for booking={} event={}: {}", bookingNumber, eventType, ex.getMessage());
        }
    }

    private JsonNode readPayload(OutboxEvent event) {
        try {
            return objectMapper.readTree(event.getPayload());
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read booking outbox payload", ex);
        }
    }

    private BookingEventType resolveEventType(OutboxEvent event, JsonNode payload) {
        String value = textValue(payload, "eventType");
        if (!StringUtils.hasText(value)) {
            value = event.getEventType();
        }

        try {
            return BookingEventType.valueOf(value);
        } catch (Exception ex) {
            log.warn("Unsupported booking outbox event type '{}' for event {} · skipping", value, event.getId());
            return null;
        }
    }

    private boolean emailRequested(JsonNode payload) {
        JsonNode node = payload.get("emailRequested");
        return node != null && node.asBoolean(false);
    }

    private boolean isEmailable(BookingEventType eventType) {
        return switch (eventType) {
            case BOOKING_CREATED,
                 BOOKING_CONFIRMED,
                 BOOKING_APPROVED,
                 BOOKING_REJECTED,
                 BOOKING_STARTED,
                 BOOKING_CHECKED_IN,
                 BOOKING_CHECKED_OUT,
                 BOOKING_COMPLETED,
                 BOOKING_CANCELLED,
                 BOOKING_NO_SHOW,
                 BOOKING_RESCHEDULED,
                 BOOKING_TRANSFERRED,
                 BOOKING_RESOURCE_CHANGED -> true;
            default -> false;
        };
    }

    private String textValue(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }
}
