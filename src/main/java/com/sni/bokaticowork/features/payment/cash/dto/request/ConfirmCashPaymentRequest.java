package com.sni.bokaticowork.features.payment.cash.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * Ce que la caisse a compte.
 *
 * <p>La session de caisse est exigee · un encaissement en especes qui n atterrit dans aucune
 * caisse rend le comptage de fin de journee faux, et personne n a de compte a rendre.</p>
 *
 * <p>Le montant est facultatif · sans lui, c est celui annonce par le client. Le renseigner sert
 * au cas frequent ou la personne ne regle qu une partie de ce qu elle avait annonce.</p>
 */
public record ConfirmCashPaymentRequest(
        @NotBlank String cashSessionNumber,
        BigDecimal amount,
        String receivedBy,
        String note
) {
}
