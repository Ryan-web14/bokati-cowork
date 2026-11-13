package com.sni.bokaticowork.features.billing.dunning.service;

import java.util.Optional;

/**
 * Lots C11 et C12 : raison de ne pas relancer une facture pour l'instant (reclamation en cours,
 * promesse de paiement non echue). Le premier motif trouve suspend le palier de relance.
 */
public interface DunningHold {

    Optional<String> holdReason(String documentNumber);
}
