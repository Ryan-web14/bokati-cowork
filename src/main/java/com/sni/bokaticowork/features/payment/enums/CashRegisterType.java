package com.sni.bokaticowork.features.payment.enums;

/** Type de caisse (plan des caisses, section 4.1). */
public enum CashRegisterType {
    /** Guichet : sessions par caissier, encaissements clients, petites depenses. */
    COUNTER,
    /** Caisse principale : alimente les guichets, recoit leurs remises, remet en banque. */
    MAIN,
    /** Petite caisse a fonds fixe : depenses sur piece, realimentee du total justifie. */
    PETTY_CASH,
    /** Coffre : pas de session, uniquement des transferts internes. */
    SAFE,
    /** Caisse automatique tenue par le systeme : aucune saisie humaine. */
    AUTOMATIC
}
