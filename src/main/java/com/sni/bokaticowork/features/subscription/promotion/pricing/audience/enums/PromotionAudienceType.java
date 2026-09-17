package com.sni.bokaticowork.features.subscription.promotion.pricing.audience.enums;

/**
 * A qui une promotion s'adresse.
 *
 * <p>Distinct des conditions, qui decrivent ce qui doit etre vrai du panier. L'audience dit qui a le
 * droit de voir l'offre ; les conditions disent quand elle s'applique. Une offre reservee a quarante
 * invites nommes peut parfaitement exiger en plus un panier minimum.</p>
 */
public enum PromotionAudienceType {

    /** Tout le monde. C'est l'audience par defaut d'une campagne publique. */
    ALL,

    /** Un segment commercial. */
    SEGMENT,

    /** Une liste de personnes nommees, tenue dans {@code promotion_beneficiary}. */
    SUBSCRIBER_LIST,

    /** Une entreprise cliente et ses membres. */
    BUSINESS_ENTITY,

    /** Les souscripteurs d'un plan donne. */
    PLAN,

    /** Une cohorte definie par une regle · les inscrits entre deux dates, par exemple. */
    COHORT
}
