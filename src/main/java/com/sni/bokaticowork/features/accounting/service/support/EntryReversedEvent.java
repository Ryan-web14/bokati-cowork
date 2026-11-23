package com.sni.bokaticowork.features.accounting.service.support;

/**
 * Lot K1 (S1) : une piece produite par un evenement comptable a ete contre-passee hors du moteur
 * (a la main, par une correction ou pour la recomptabiliser). Le moteur passe l'evenement en
 * {@code REVERSED} et previent le module source.
 */
public record EntryReversedEvent(Long postingEventId, String provisionalNumber, String reversalNumber, String reason) {
}
