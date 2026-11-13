package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Remise en circuit d'un devis expire avec une nouvelle date de validite (decision D4). */
public record RevalidateQuoteRequest(@NotNull LocalDate validUntil, @NotBlank @Size(max = 1000) String reason) {
}
