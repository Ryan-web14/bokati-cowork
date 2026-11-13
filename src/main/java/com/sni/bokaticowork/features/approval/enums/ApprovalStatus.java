package com.sni.bokaticowork.features.approval.enums;

/** Etat d'une demande d'approbation (lot B15). */
public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
    /** Renvoyee au demandeur pour correction ; une nouvelle soumission ouvre une nouvelle demande. */
    RETURNED,
    CANCELLED
}
