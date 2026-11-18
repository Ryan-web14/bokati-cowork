package com.sni.bokaticowork.features.booking.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Lot B2b : apercu d'un report ou d'un changement de ressource, avant de le valider.
 *
 * @param change            INCREASE (plus cher : confirmation et encaissement du complement),
 *                          DECREASE (moins cher : avoir) ou SAME
 * @param invoiced          la reservation est deja facturee (l'ecart passera par un complement ou un avoir)
 * @param modificationCount nombre de reports et changements deja faits
 */
public record BookingChangePreviewResponse(String bookingNumber, BigDecimal currentAmount, BigDecimal newAmount,
                                           BigDecimal difference, String change, String currency, boolean invoiced,
                                           int modificationCount, Instant lastModifiedAt, String lastModification) {
}
