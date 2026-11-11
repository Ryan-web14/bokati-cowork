package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/** Plage d'ouverture : jour (1 = lundi, 7 = dimanche), heure d'ouverture et de fermeture. */
public record CashRegisterScheduleRequest(
        @NotNull @Min(1) @Max(7) Integer dayOfWeek,
        @NotNull LocalTime opensAt,
        @NotNull LocalTime closesAt
) {
}
