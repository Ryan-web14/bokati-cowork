package com.sni.bokaticowork.features.booking.dto.request;

import java.time.Instant;

/**
 * Lot B7 : modification d'une derogation de quota. Le titulaire ne change pas (creer une autre
 * derogation) ; tout le reste est remplace par ce qui est envoye.
 */
public record UpdateBookingQuotaOverrideRequest(
        String resourceCode,
        Integer extraActiveBookings,
        Integer extraBookingsPerDay,
        Integer extraBookingsPerWeek,
        Integer extraBookingsPerMonth,
        String reason,
        Instant validFrom,
        Instant validUntil
) {
}
