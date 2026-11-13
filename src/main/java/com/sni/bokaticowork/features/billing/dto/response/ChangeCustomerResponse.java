package com.sni.bokaticowork.features.billing.dto.response;

import java.math.BigDecimal;

/**
 * Resultat d'un changement de client (lot B6).
 *
 * @param mode               IN_PLACE (meme facture, nouvelle version) ou REISSUE (avoirs et nouvelle facture)
 * @param unpaidCreditNote   avoir qui annule le reste du de la facture d'origine
 * @param paidCreditNote     avoir des reglements recus (garde au client d'origine, ou transfere)
 */
public record ChangeCustomerResponse(
        String mode,
        String originalInvoice,
        String newInvoice,
        String unpaidCreditNote,
        String paidCreditNote,
        String paymentsMode,
        BigDecimal transferredAmount
) {
}
