package com.sni.bokaticowork.features.payment.provider;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Ce que l'operateur repond a une demande de depot.
 *
 * <p>{@code PROCESSING} la demande est partie, {@code FAILED} elle est refusee, {@code UNKNOWN}
 * l'operateur n'a pas repondu · dans ce dernier cas la demande a peut-etre ete recue, donc on
 * n'ecrit surtout pas un echec : on relit le statut.</p>
 */
public record MobileMoneyInitiationResponse(
        String providerReference,
        String status,
        String message,
        JsonNode failureReason
) {
    public MobileMoneyInitiationResponse(String providerReference, String status, String message) {
        this(providerReference, status, message, null);
    }

    public boolean unknown() {
        return "UNKNOWN".equals(status);
    }
}
