package com.sni.bokaticowork.features.accounting.service.support;

import com.sni.bokaticowork.features.accounting.model.AccPostingEvent;

/**
 * Prevenu du resultat de la comptabilisation d'un evenement (dans la transaction du moteur), pour
 * reporter l'etat comptable sur le fait d'origine (mouvement de caisse, transaction, facture).
 */
public interface PostingOutcomeListener {

    boolean supports(String eventType);

    void onOutcome(AccPostingEvent event);
}
