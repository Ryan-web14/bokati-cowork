package com.sni.bokaticowork.features.booking.dto.response;

import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingAvailabilityResponse(
        String resourceCode,
        String resourceName,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer durationMinutes,
        Integer quantity,
        boolean available,
        Integer remainingCapacity,
        ResourceBookingUnit bookingUnit,
        BigDecimal unitPrice,
        BigDecimal estimatedAmount,
        String currency,
        String message,
        /**
         * Creneau indisponible : plages occupees qui le chevauchent, chacune en entier (de son debut a
         * sa fin), avec les reservations en cause. Vide si le creneau est disponible.
         */
        List<OccupiedRange> occupiedRanges
) {

    /** Plage occupee : {@code BOOKED} (reservations), ou {@code UNAVAILABLE} (fermeture, option, creneau ferme). */
    public record OccupiedRange(LocalDateTime startedAt, LocalDateTime endedAt, String reason, List<String> bookingNumbers) {
    }

    public BookingAvailabilityResponse(String resourceCode, String resourceName, LocalDateTime startedAt, LocalDateTime endedAt,
                                       Integer durationMinutes, Integer quantity, boolean available, Integer remainingCapacity,
                                       ResourceBookingUnit bookingUnit, BigDecimal unitPrice, BigDecimal estimatedAmount, String currency,
                                       String message) {
        this(resourceCode, resourceName, startedAt, endedAt, durationMinutes, quantity, available, remainingCapacity, bookingUnit, unitPrice,
                estimatedAmount, currency, message, List.of());
    }

    /** Meme reponse sans les numeros de reservation : le portail ne montre pas les reservations des autres. */
    public BookingAvailabilityResponse withoutBookingNumbers() {
        List<OccupiedRange> ranges = occupiedRanges == null ? List.of() : occupiedRanges.stream()
                .map(range -> new OccupiedRange(range.startedAt(), range.endedAt(), range.reason(), null)).toList();
        return new BookingAvailabilityResponse(resourceCode, resourceName, startedAt, endedAt, durationMinutes, quantity, available,
                remainingCapacity, bookingUnit, unitPrice, estimatedAmount, currency, message, ranges);
    }
}
