package com.sni.bokaticowork.features.billing.dto.response;

import java.util.List;

/**
 * Calcul d'une ligne en cours de saisie et du document qui la contient (lot B13).
 *
 * @param alerts alertes propres a la ligne (prix plancher, remise maximale, stock)
 */
public record SimulateBillingLineResponse(
        int lineIndex,
        SimulateBillingDocumentResponse.SimulatedLine line,
        List<SimulateBillingDocumentResponse.Alert> alerts,
        SimulateBillingDocumentResponse document
) {
}
