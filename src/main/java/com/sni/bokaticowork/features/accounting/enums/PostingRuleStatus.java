package com.sni.bokaticowork.features.accounting.enums;

/** Etat d'une version de schema d'ecriture. */
public enum PostingRuleStatus {
    /** En preparation : ne s'applique pas. */
    DRAFT,
    /** Applique aux evenements de son type a partir de sa date d'effet. */
    ACTIVE,
    /** Remplace par une version plus recente. */
    RETIRED
}
