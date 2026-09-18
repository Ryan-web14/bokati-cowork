package com.sni.bokaticowork.features.subscription.subscription.pass.enums;

/**
 * Ce qu'une regle de validite peut restreindre.
 *
 * <p>Toutes s'expriment en donnees. Un pass valable en semaine, un pass heures creuses, un pass
 * limite a un site, un pass bloque les jours feries, un pass a une visite par jour : aucun de ces
 * cas ne devrait demander une livraison.</p>
 */
public enum PassValidityRuleType {
    DAY_OF_WEEK,
    TIME_RANGE,
    LOCATION,
    RESOURCE_TYPE,
    BLACKOUT_DATE,
    MAX_PER_DAY,
    MAX_CONCURRENT
}
