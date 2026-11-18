package com.sni.bokaticowork.features.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record BookingRescheduleRequest(
        @NotNull LocalDateTime newStartedAt,
        @NotNull LocalDateTime newEndedAt,
        String actor,
        String note,
        Boolean sendEmail,
        /** Lot B2b : le nouveau prix est plus eleve ; l'equipe confirme la hausse (complement a encaisser). */
        Boolean acceptPriceChange
) {
    public BookingRescheduleRequest(LocalDateTime newStartedAt, LocalDateTime newEndedAt, String actor, String note, Boolean sendEmail) {
        this(newStartedAt, newEndedAt, actor, note, sendEmail, null);
    }
}
