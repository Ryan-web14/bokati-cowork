package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.model.BookingEvent;
import com.sni.bokaticowork.features.booking.repository.BookingEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BookingEventWriter {

    private final BookingEventRepository eventRepository;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Transactional
    public BookingEvent write(Booking booking, BookingEventType type, String title, String description, String payloadJson) {
        return eventRepository.save(BookingEvent.builder()
                .eventNumber(sequenceGenerator.next("booking_event"))
                .booking(booking)
                .eventType(type)
                .title(title)
                .description(description)
                .payloadJson(payloadJson)
                .build());
    }
}
