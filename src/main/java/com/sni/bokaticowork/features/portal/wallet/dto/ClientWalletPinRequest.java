package com.sni.bokaticowork.features.portal.wallet.dto;

/**
 * Le code secret, quand un paiement l'exige.
 *
 * <p>Corps facultatif : par defaut aucun paiement vers l'etablissement ne demande le code, et une
 * interface qui ne le connait pas doit pouvoir payer sans l'envoyer. Si la politique l'exige, le
 * refus le dira et l'interface le demandera.</p>
 */
public record ClientWalletPinRequest(String pin) {
}
