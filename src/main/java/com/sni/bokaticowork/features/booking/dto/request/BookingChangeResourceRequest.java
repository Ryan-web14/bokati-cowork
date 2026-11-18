package com.sni.bokaticowork.features.booking.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BookingChangeResourceRequest(
        @NotBlank String newResourceCode,
        String actor,
        String note,
        Boolean sendEmail,
        /** Lot B2b : le nouveau prix est plus eleve ; l'equipe confirme la hausse (complement a encaisser). */
        Boolean acceptPriceChange
) {
    public BookingChangeResourceRequest(String newResourceCode, String actor, String note, Boolean sendEmail) {
        this(newResourceCode, actor, note, sendEmail, null);
    }
}
