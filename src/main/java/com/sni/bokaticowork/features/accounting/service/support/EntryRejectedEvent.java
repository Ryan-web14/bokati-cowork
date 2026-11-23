package com.sni.bokaticowork.features.accounting.service.support;

/**
 * Lot K1 (S3) : un brouillard ou une piece en attente produit par un evenement comptable a ete
 * rejete. L'evenement repasse en echec avec le motif : il se regenere ou s'ecarte.
 */
public record EntryRejectedEvent(Long postingEventId, String provisionalNumber, String reason) {
}
