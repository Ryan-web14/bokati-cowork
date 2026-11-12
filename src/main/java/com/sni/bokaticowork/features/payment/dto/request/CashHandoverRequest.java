package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Passation : le sortant compte et designe l'entrant. */
public record CashHandoverRequest(@NotBlank String incomingEmail, @NotNull @Valid CashCountRequest count) {
}
