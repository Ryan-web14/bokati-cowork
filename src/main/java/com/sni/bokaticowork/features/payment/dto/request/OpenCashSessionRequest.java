package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * Ouverture d'une session de caisse.
 *
 * @param openedBy             ignore en contexte authentifie : le caissier est l'utilisateur connecte
 * @param openingAmount        fonds compte, si aucun billetage n'est envoye
 * @param deviceCode           poste depuis lequel on ouvre ; exige si la caisse a des postes autorises
 * @param outOfHoursReasonCode motif (liste {@code OUT_OF_HOURS_REASON}) d'une ouverture hors horaires
 * @param outOfHoursReasonText precision, obligatoire pour le motif {@code OTHER}
 * @param openingCount         billetage d'ouverture ; exige en cas d'ecart si la caisse impose le billetage
 */
public record OpenCashSessionRequest(
        @NotBlank String registerCode,
        String openedBy,
        BigDecimal openingAmount,
        String deviceCode,
        String outOfHoursReasonCode,
        String outOfHoursReasonText,
        @Valid CashCountRequest openingCount
) {
    public OpenCashSessionRequest(String registerCode, String openedBy, BigDecimal openingAmount) {
        this(registerCode, openedBy, openingAmount, null, null, null, null);
    }
}
