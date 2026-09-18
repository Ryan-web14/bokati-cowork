package com.sni.bokaticowork.features.subscription.subscription.pass.enums;

public enum PassUsageStatus {

    /** Droit retenu, usage pas encore effectif. Une salle reservee pour demain. */
    RESERVED,

    /** En cours · quelqu'un est sur place. */
    ACTIVE,

    COMPLETED,
    CANCELLED,
    NO_SHOW
}
