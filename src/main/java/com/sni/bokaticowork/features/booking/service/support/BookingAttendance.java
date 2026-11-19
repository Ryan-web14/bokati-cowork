package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.booking.dto.request.BookingStatusChangeRequest;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

/**
 * Lot B9a : reponse du client au rappel, « je viens » ou « j'annule », par les liens a jeton du
 * courriel de rappel (meme jeton que la confirmation). « Je viens » est note sur la reservation ;
 * « j'annule » passe par l'annulation ordinaire, politique de la ressource comprise.
 */
@Component
@RequiredArgsConstructor
public class BookingAttendance {

    public static final String COMING = "COMING";
    public static final String CANCELLED = "CANCELLED";

    private final BookingRepository bookings;
    private final BookingEventWriter events;
    private final ObjectProvider<BookingService> bookingService;

    /** La reservation du lien, si le jeton est le sien ; sinon introuvable. */
    @Transactional(readOnly = true)
    public Booking verified(String bookingNumber, String token) {
        Booking booking = bookings.findByBookingNumber(bookingNumber).orElse(null);
        if (booking == null || !StringUtils.hasText(token) || !token.equals(booking.getCheckInToken())) {
            throw new ResourceNotFoundException("Réservation introuvable");
        }
        return booking;
    }

    @Transactional
    public Booking coming(String bookingNumber, String token) {
        Booking booking = verified(bookingNumber, token);
        requireUpcoming(booking);
        if (!COMING.equals(booking.getAttendanceAnswer())) {
            booking.setAttendanceAnswer(COMING);
            booking.setAttendanceAnsweredAt(Instant.now());
            bookings.save(booking);
            events.write(booking, BookingEventType.BOOKING_ATTENDANCE_CONFIRMED, "Venue confirmée par le client", "Réponse au rappel : je viens", null);
        }
        return booking;
    }

    @Transactional
    public Booking cancel(String bookingNumber, String token) {
        Booking booking = verified(bookingNumber, token);
        requireUpcoming(booking);
        bookingService.getObject().cancel(bookingNumber, new BookingStatusChangeRequest("CLIENT",
                "Annulée par le client depuis le rappel", Boolean.TRUE));
        Booking cancelled = bookings.findByBookingNumber(bookingNumber).orElse(booking);
        cancelled.setAttendanceAnswer(CANCELLED);
        cancelled.setAttendanceAnsweredAt(Instant.now());
        return bookings.save(cancelled);
    }

    /** Lot B9a : reponse depuis l'espace client (le titulaire est deja verifie). */
    @Transactional
    public Booking answer(String bookingNumber, boolean coming) {
        Booking booking = bookings.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable : " + bookingNumber));
        return coming ? coming(bookingNumber, booking.getCheckInToken()) : cancel(bookingNumber, booking.getCheckInToken());
    }

    private static void requireUpcoming(Booking booking) {
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new ConflictException("Cette réservation est " + booking.getStatus().label() + " : le lien n'a plus d'effet");
        }
    }
}
