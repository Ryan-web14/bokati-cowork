package com.sni.bokaticowork.features.payment.enums;

public enum CashAnomalyType {
    VARIANCE_OUTLIER,
    ROUND_NUMBER_PATTERN,
    EXCESSIVE_REFUNDS,
    EXCESSIVE_ADJUSTMENTS,
    OFF_HOURS_SESSION,
    THRESHOLD_STRUCTURING,
    NEAR_MAX_CASH_RECURRENCE,
    /** Lot C4 : fonds compte a l'ouverture different du dernier comptage. */
    OPENING_VARIANCE,
    /** Lot C4 : session ouverte au-dela de la duree maximale de sa caisse. */
    LONG_SESSION,
    /** Lot C4 : pauses longues ou repetees. */
    LONG_PAUSES,
    /** Lot C4 : contre-passations repetees par le meme caissier. */
    REPEATED_REVERSALS,
    /** Lot C4 : solde au-dela du plafond de detention. */
    DETENTION_CAP_EXCEEDED,
    /** Lot C4 : session ouverte depuis un poste inconnu ou refuse. */
    UNKNOWN_DEVICE,
    /** Lot C4 : billets abimes ou suspects comptes. */
    DAMAGED_NOTES,
    /** Lot C4 : transfert recu pour un autre montant que l'envoi. */
    TRANSFER_VARIANCE
}
