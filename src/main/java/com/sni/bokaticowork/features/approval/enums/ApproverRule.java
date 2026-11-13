package com.sni.bokaticowork.features.approval.enums;

/** Qui decide d'un niveau. */
public enum ApproverRule {
    /** Le N-ieme superieur du demandeur (N+1, N+2...), et les detenteurs du droit du niveau s'il en a un. */
    MANAGER,
    /** Les detenteurs d'un droit. */
    PERMISSION,
    /** Un utilisateur nomme. */
    USER
}
