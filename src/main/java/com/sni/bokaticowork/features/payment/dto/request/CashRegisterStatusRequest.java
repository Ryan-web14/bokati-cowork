package com.sni.bokaticowork.features.payment.dto.request;

/**
 * Suspension, reprise ou fermeture d'une caisse.
 *
 * @param reasonCode code de la liste {@code SUSPENSION_REASON} (obligatoire pour suspendre)
 * @param reasonText precision, obligatoire si le code est {@code OTHER}
 */
public record CashRegisterStatusRequest(String reasonCode, String reasonText) {
}
