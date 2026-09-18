package com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.enums;

/** A qui une grille s'adresse. */
public enum PriceListAudienceType {

    /** Tarif public applicable a tous · une grille saisonniere, par exemple. */
    ALL,

    /** Un souscripteur nomme. */
    SUBSCRIBER,

    /** Un segment commercial · etudiant, association. */
    SEGMENT,

    /** Une entreprise cliente et ses membres. */
    BUSINESS_ENTITY,

    /** Un partenaire institutionnel dont les membres beneficient du tarif negocie. */
    PARTNER
}
