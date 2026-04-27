package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.model.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class BookingEmailNotifier {

    private final DefaultEmailSender emailSender;
    private final BookingEventWriter eventWriter;

    public void notify(Booking booking, BookingEventType eventType) {
        if (!StringUtils.hasText(booking.getContactEmail())) {
            return;
        }
        String subject = subject(eventType, booking);
        String body = body(eventType, booking);
        eventWriter.write(booking, BookingEventType.EMAIL_QUEUED, "Email queued", subject, null);
        emailSender.sendEmail(booking.getContactEmail(), subject, body)
                .thenAccept(sent -> eventWriter.write(
                        booking,
                        sent ? BookingEventType.EMAIL_SENT : BookingEventType.EMAIL_FAILED,
                        sent ? "Email sent" : "Email failed",
                        subject,
                        null
                ));
    }

    private String subject(BookingEventType eventType, Booking booking) {
        return switch (eventType) {
            case BOOKING_CONFIRMED -> "Booking confirmed " + booking.getBookingNumber();
            case BOOKING_CANCELLED -> "Booking cancelled " + booking.getBookingNumber();
            case BOOKING_COMPLETED -> "Booking completed " + booking.getBookingNumber();
            case BOOKING_NO_SHOW -> "Booking no-show " + booking.getBookingNumber();
            default -> "Booking update " + booking.getBookingNumber();
        };
    }

    private String body(BookingEventType eventType, Booking booking) {
        return """
                Booking: %s
                Resource: %s
                Status: %s
                Start: %s
                End: %s
                Event: %s
                """.formatted(
                booking.getBookingNumber(),
                booking.getResource().getName(),
                booking.getStatus(),
                booking.getStartedAt(),
                booking.getEndedAt(),
                eventType
        );
    }
}
