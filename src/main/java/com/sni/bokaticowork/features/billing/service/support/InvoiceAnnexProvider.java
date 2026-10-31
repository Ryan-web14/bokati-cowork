package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;

import java.util.List;

/**
 * Fournit l'annexe d'une facture creee depuis des elements facturables · vide si elle n'en a pas.
 *
 * <p>Interroge a la creation, avant l'emission. La facturation ignore tout du partage d'un
 * abonnement : c'est le module des organisations qui sait lire la consommation des places.</p>
 */
public interface InvoiceAnnexProvider {

    InvoiceUsageAnnex annexFor(List<BillableItem> items);
}
