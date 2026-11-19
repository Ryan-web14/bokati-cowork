package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.portal.notification.service.AdminInAppNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;

/**
 * Lot B9g : avis apres utilisation. La note (1 a 5) arrive par le lien du courriel de cloture ;
 * le client peut ajouter un commentaire. Une note basse ({@code bookings.reviews.alert-threshold},
 * 2) alerte l'equipe en direct ({@code BOOKING_LOW_REVIEW}).
 */
@Slf4j
@Component
public class BookingReviews {

    static final String THRESHOLD_KEY = "bookings.reviews.alert-threshold";

    private final BookingRepository bookings;

    private PlatformSettings settings;
    private AdminInAppNotifier adminNotifier;

    public BookingReviews(BookingRepository bookings) {
        this.bookings = bookings;
    }

    @Autowired(required = false)
    void setOptional(PlatformSettings settings, @Lazy AdminInAppNotifier adminNotifier) {
        this.settings = settings;
        this.adminNotifier = adminNotifier;
    }

    /** Note enregistree : horodatee, alerte si basse. */
    @Transactional
    public void rated(Booking booking) {
        booking.setCsatAt(Instant.now());
        bookings.save(booking);
        alertIfLow(booking);
    }

    /** Commentaire ajoute a l'avis ; rend la note. */
    @Transactional
    public int comment(String bookingNumber, String token, String comment) {
        Booking booking = bookings.findByBookingNumber(bookingNumber).orElse(null);
        if (booking == null || !StringUtils.hasText(token) || !token.equals(booking.getCheckInToken())) {
            throw new ResourceNotFoundException("Réservation introuvable");
        }
        if (booking.getCsatScore() == null) {
            throw new BadRequestException("Donnez d'abord une note");
        }
        String text = comment == null ? "" : comment.trim();
        booking.setCsatComment(text.isEmpty() ? null : text.length() > 1000 ? text.substring(0, 1000) : text);
        bookings.save(booking);
        alertIfLow(booking);
        return booking.getCsatScore();
    }

    private void alertIfLow(Booking booking) {
        int threshold = settings == null ? 2 : settings.integer(THRESHOLD_KEY, 2);
        if (adminNotifier == null || booking.getCsatScore() == null || booking.getCsatScore() > threshold) {
            return;
        }
        try {
            String resource = booking.getResource() == null ? "" : booking.getResource().getName();
            String message = "Note " + booking.getCsatScore() + "/5 pour " + resource + " (réservation " + booking.getBookingNumber() + ")"
                    + (StringUtils.hasText(booking.getCsatComment()) ? " : " + booking.getCsatComment() : "");
            String payload = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of(
                    "bookingNumber", booking.getBookingNumber(), "score", booking.getCsatScore(),
                    "resourceCode", booking.getResource() == null ? "" : booking.getResource().getCode()));
            adminNotifier.broadcastAlert("BOOKING_LOW_REVIEW", "BOOKING", "Avis négatif sur une réservation", message, "WARNING", payload);
        } catch (Exception ex) {
            log.warn("Alerte d'avis de {} non envoyee : {}", booking.getBookingNumber(), ex.getMessage());
        }
    }
}
