package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Mise sous scelles (ou ouverture du scelle) : comptage a deux, enveloppe numerotee, lieu de depot. */
public record CashSealRequest(
        String envelopeNumber,
        String depositedIn,
        @NotBlank String witnessEmail,
        @NotNull @Valid CashCountRequest count,
        String absenceNumber
) {
}
