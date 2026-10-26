package com.sni.bokaticowork.features.payment.provider;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Ce que l'operateur dit d'un depot · y compris « je ne sais pas ».
 *
 * <p>{@code status} vaut {@code SUCCEEDED}, {@code FAILED}, {@code PROCESSING}, {@code NOT_FOUND}
 * (le depot n'a jamais atteint l'operateur) ou {@code UNKNOWN} (l'operateur n'a pas repondu).
 * {@code UNKNOWN} ne doit produire aucune ecriture · c'est une absence de reponse, pas un echec.</p>
 */
public record MobileMoneyStatusResponse(
        String providerReference,
        String status,
        String message,
        JsonNode failureReason,
        String providerTransactionId,
        // Ce que l'operateur dit avoir encaisse · compare au depot avant de declarer le paiement recu.
        String amount,
        String currency
) {
    public MobileMoneyStatusResponse(String providerReference, String status, String message) {
        this(providerReference, status, message, null, null);
    }

    public MobileMoneyStatusResponse(String providerReference, String status, String message,
                                     JsonNode failureReason, String providerTransactionId) {
        this(providerReference, status, message, failureReason, providerTransactionId, null, null);
    }

    public boolean unknown() {
        return "UNKNOWN".equals(status);
    }

    public boolean terminal() {
        return "SUCCEEDED".equals(status) || "FAILED".equals(status) || "NOT_FOUND".equals(status);
    }
}
