package com.sni.bokaticowork.features.payment.enums;

/** Nature d'un transfert interne. */
public enum CashTransferKind {
    /** Alimentation d'une caisse par sa caisse mere. */
    FEED,
    /** Remise d'une caisse a sa caisse mere (fin de journee, plafond). */
    REMITTANCE,
    /** Depot au coffre. */
    SAFE_DEPOSIT,
    /** Retrait du coffre. */
    SAFE_WITHDRAWAL,
    /** Realimentation de la petite caisse du total des depenses justifiees. */
    PETTY_REFILL,
    /** Autre transfert autorise. */
    OTHER
}
