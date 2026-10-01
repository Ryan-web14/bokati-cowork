package com.sni.bokaticowork.features.billing.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Le releve d un client · ce qu il doit, ce qu on lui doit, et ce qui ne compte pas encore.
 *
 * <p>{@code totalBalanceDue} ne retenait auparavant que le type de document : une facture en
 * brouillon, jamais presentee, y figurait comme due. La liste {@code documents} melange par
 * ailleurs tous les types, avoirs compris · un consommateur qui sommait leur {@code balanceDue}
 * reclamait au client ce qu on lui devait. Chaque document porte desormais son
 * {@code customerImpact}, signe et deja neutralise pour ce qui ne compte pas.</p>
 */
public record CustomerStatementResponse(
        String customerType,
        String customerCode,
        /** Ce qui a ete facture au client · les brouillons n en font pas partie. */
        BigDecimal totalInvoiced,
        BigDecimal totalPaid,
        /** Ce qui reste du sur les factures emises et non reglees · rien d autre. */
        BigDecimal totalBalanceDue,
        /** Les avoirs scelles et pas encore consommes · ce que nous devons au client. */
        BigDecimal totalCreditAvailable,
        /** Ce qu il reste a reclamer une fois les avoirs deduits · jamais negatif. */
        BigDecimal netBalanceDue,
        /** Les factures en preparation · pour information, hors de tout solde. */
        BigDecimal totalDraft,
        List<BillingDocumentResponse> documents
) {
}
