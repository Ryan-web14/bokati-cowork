package com.sni.bokaticowork.features.portal.subscription.dto.response;

import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.SubscriptionResponse;

/**
 * Ce que le client obtient en renouvelant · sa nouvelle periode, et la facture a regler.
 *
 * <p>Rendre les deux ensemble evite l aller-retour : l ecran affiche la periode reconduite et
 * propose de payer la facture dans le meme mouvement. Sans la facture, le client devait aller la
 * chercher dans sa liste, et beaucoup repartaient sans payer.</p>
 */
public record ClientSubscriptionRenewalResponse(
        SubscriptionResponse subscription,
        /** La facture de renouvellement · nulle si rien n etait a facturer. */
        BillingDocumentResponse invoice
) {
}
