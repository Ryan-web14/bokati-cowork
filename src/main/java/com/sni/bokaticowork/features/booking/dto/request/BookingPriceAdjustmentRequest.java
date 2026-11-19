package com.sni.bokaticowork.features.booking.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Lot B4 (decision D4) : ajustement manuel du prix par l'equipe, a la creation ou ensuite.
 *
 * <p>Un seul de {@code discountRate} (remise en %, de 0,01 a 100) ou {@code newAmount} (nouveau
 * total, inferieur au prix calcule) ; le motif vient de la liste {@code BOOKING_PRICE_ADJUSTMENT_REASON}
 * ({@code reasonCode}, plus {@code reasonText} pour « Autre »). Une hausse n'est pas un ajustement :
 * elle passe par l'unite ou la grille tarifaire.</p>
 */
public record BookingPriceAdjustmentRequest(
        @DecimalMin(value = "0.01") @DecimalMax(value = "100") BigDecimal discountRate,
        @DecimalMin(value = "0") BigDecimal newAmount,
        @Size(max = 60) String reasonCode,
        @Size(max = 500) String reasonText
) {
}
