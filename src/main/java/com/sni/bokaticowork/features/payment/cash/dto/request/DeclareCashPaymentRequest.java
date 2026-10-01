package com.sni.bokaticowork.features.payment.cash.dto.request;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Ce que le client annonce · un montant s il ne regle qu une partie, un mot s il veut en dire un.
 *
 * <p>Tout est facultatif : sans montant, c est le solde de la facture ; sans mot, rien.</p>
 */
public record DeclareCashPaymentRequest(
        BigDecimal amount,
        @Size(max = 500) String note
) {
}
