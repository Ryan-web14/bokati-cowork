package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Escompte pour paiement anticipé.
 * Affiché sur le document : "Escompte de X% si règlement avant le JJ/MM/AAAA".
 */
public record CreateEarlyPaymentDiscountRequest(
        @NotNull @Positive BigDecimal discountRate,
        @NotNull LocalDate ifPaidBefore,
        String label
) {
}
