package com.sni.bokaticowork.core.outbox.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.support.BookingEmailNotifier;
import com.sni.bokaticowork.features.booking.service.support.BookingEventOutboxPayload;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingOutboxEventProcessorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final BookingEmailNotifier emailNotifier = mock(BookingEmailNotifier.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<BookingEmailNotifier> emailNotifierProvider = mock(ObjectProvider.class);
    private final BookingOutboxEventProcessor processor = new BookingOutboxEventProcessor(
            objectMapper,
            bookingRepository,
            emailNotifierProvider
    );

    @Test
    void shouldQueueBookingEmailWhenRequested() throws Exception {
        Booking booking = Booking.builder()
                .bookingNumber("BKG-001")
                .contactEmail("jane@example.com")
                .build();
        OutboxEvent event = outboxEvent(true);

        when(bookingRepository.findPublicByBookingNumberWithResource("BKG-001")).thenReturn(Optional.of(booking));
        when(emailNotifierProvider.getObject()).thenReturn(emailNotifier);

        processor.process(event);

        verify(emailNotifier).notify(booking, BookingEventType.BOOKING_CONFIRMED);
    }

    @Test
    void shouldIgnoreBookingEmailWhenNotRequested() throws Exception {
        OutboxEvent event = outboxEvent(false);

        processor.process(event);

        verify(emailNotifierProvider, never()).getObject();
        verify(bookingRepository, never()).findPublicByBookingNumberWithResource("BKG-001");
    }

    private OutboxEvent outboxEvent(boolean emailRequested) throws Exception {
        BookingEventOutboxPayload payload = new BookingEventOutboxPayload(
                "BEV-001",
                "BKG-001",
                BookingEventType.BOOKING_CONFIRMED.name(),
                "Booking confirmed",
                "Booking was confirmed",
                null,
                emailRequested
        );

        return OutboxEvent.builder()
                .id(1L)
                .eventType(BookingEventType.BOOKING_CONFIRMED.name())
                .aggregateType("BOOKING")
                .aggregateId("BKG-001")
                .payload(objectMapper.writeValueAsString(payload))
                .build();
    }
}
