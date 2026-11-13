package com.sni.bokaticowork.features.approval.enums;

/** Etat d'un niveau d'une demande d'approbation. */
public enum ApprovalStepStatus {
    /** Niveau pas encore atteint. */
    WAITING,
    /** Niveau courant : attend sa decision. */
    PENDING,
    APPROVED,
    /** Niveau saute : un niveau superieur a approuve en avance (decision D11). */
    APPROVED_BY_HIGHER,
    REJECTED,
    RETURNED,
    CANCELLED
}
