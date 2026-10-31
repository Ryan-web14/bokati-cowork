package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;

import java.util.List;

/**
 * Fournit l'annexe d'une facture creee depuis des elements facturables · vide si elle n'en a pas.
 *
 * <p>Interroge a la creation, avant l'emission. La facturation ignore tout du partage d'un
 * abonnement : c'est le module des organisations qui sait lire la consommation des places.</p>
 *
 * <p>Une implementation lit dans sa propre transaction en lecture seule
 * ({@code Propagation.REQUIRES_NEW}) : une erreur de base pendant sa lecture n'y condamne que
 * celle-la, et la facture sort sans annexe au lieu d'echouer avec elle.</p>
 */
public interface InvoiceAnnexProvider {

    InvoiceUsageAnnex annexFor(List<BillableItem> items);
}
