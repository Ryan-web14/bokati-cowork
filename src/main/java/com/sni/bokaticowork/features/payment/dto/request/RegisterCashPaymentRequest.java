package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Encaissement en especes sur une intention.
 *
 * @param amount         montant a imputer ; absent, le plus petit du montant remis et du reste a payer
 * @param tenderedAmount ce que le client a tendu ; la monnaie rendue en est deduite
 */
public record RegisterCashPaymentRequest(
        /** Ignore en contexte authentifie : l'auteur est l'utilisateur connecte. */
        String receivedBy,
        @NotNull String cashSessionNumber,
        BigDecimal amount,
        String providerReference,
        String metadataJson,
        BigDecimal tenderedAmount
) {
    public RegisterCashPaymentRequest(String receivedBy, String cashSessionNumber, BigDecimal amount,
                                      String providerReference, String metadataJson) {
        this(receivedBy, cashSessionNumber, amount, providerReference, metadataJson, null);
    }
}
