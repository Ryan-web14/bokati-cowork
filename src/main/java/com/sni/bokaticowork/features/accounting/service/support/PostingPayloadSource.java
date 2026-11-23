package com.sni.bokaticowork.features.accounting.service.support;

/**
 * Lot K0 (revue comptable, M3 et M6) : source des donnees d'un evenement comptable, capable de les
 * relire depuis le fait metier. La regeneration d'un evenement relit d'abord ses donnees : une
 * facture ou un paiement corrige a la source produit une piece a jour, et un fait dont la lecture
 * avait echoue (taux de change manquant, session introuvable) est retente.
 */
public interface PostingPayloadSource {

    /** Vrai si cette source sait relire ce type d'evenement. */
    boolean supports(String eventType);

    /**
     * Relit le fait et transmet ses donnees au moteur ({@code PostingEngine.receive}), qui met a
     * jour l'evenement encore ouvert. Une exception dit pourquoi la lecture echoue.
     */
    void rebuild(String eventType, String reference);
}
