package com.sni.bokaticowork.features.billing.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Frise de negociation d'un devis (lot B2) : chaque version, son auteur, son motif, son origine,
 * et l'ecart de prix avec la precedente. La derniere est la version courante.
 */
public record QuoteNegotiationResponse(
        String quoteNumber,
        String status,
        int currentRevision,
        List<Round> rounds
) {

    /**
     * @param totalDelta     ecart du total TTC avec la version precedente (vide pour la premiere)
     * @param lineChanges    lignes ajoutees, retirees ou modifiees (prix, quantite, remise)
     */
    public record Round(
            int version,
            boolean current,
            String authorName,
            String reason,
            String origin,
            String status,
            Instant changedAt,
            Instant sentToCustomerAt,
            BigDecimal subtotalAmount,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            BigDecimal totalDelta,
            List<LineChange> lineChanges
    ) {
    }

    /** {@code change} : ADDED, REMOVED ou CHANGED ; valeurs avant et apres pour une ligne modifiee. */
    public record LineChange(String change, String description, String quantityBefore, String quantityAfter, String unitPriceBefore,
                             String unitPriceAfter, String discountBefore, String discountAfter, String totalBefore, String totalAfter) {
    }
}
