package com.sni.bokaticowork.features.accounting.service.support;

/**
 * Lot K0 (revue comptable, M1) : une piece produite par un evenement comptable vient d'etre validee,
 * par exemple apres avoir ete completee a la main alors qu'elle attendait son equilibrage. Le moteur
 * met alors l'evenement a jour (comptabilise, numero definitif) et previent les modules concernes.
 */
public record EntryValidatedEvent(Long postingEventId, String provisionalNumber, String entryNumber) {
}
