package com.sni.bokaticowork.features.payment.transfer.model;

/**
 * Cycle de vie d'un transfert.
 *
 * <p>Il n'y a pas d'etat « en cours d'execution » : les deux ecritures se font dans une seule
 * transaction de base, et l'on est soit avant, soit apres. Un transfert qu'on observerait entre les
 * deux n'existe pas.</p>
 */
public enum WalletTransferStatus {
    /** Cree, en attente du code du titulaire. */
    PENDING_CONFIRMATION,
    COMPLETED,
    /** Confirmation refusee, plafond depasse a l'execution, solde insuffisant. */
    FAILED,
    /** Abandonne par le titulaire avant confirmation, ou echu. */
    CANCELLED
}
