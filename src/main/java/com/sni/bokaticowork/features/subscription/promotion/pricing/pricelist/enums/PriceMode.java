package com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.enums;

/**
 * Comment une ligne de grille determine le prix.
 *
 * <p>Les trois modes different par ce qu'ils font du prix catalogue, et ce n'est pas indifferent :
 * un prix fixe ignore le catalogue et ne bouge pas quand celui-ci augmente, les deux autres en
 * derivent et suivent donc ses evolutions.</p>
 */
public enum PriceMode {

    /** Remplace le prix, quel que soit le catalogue. Un tarif au poste negocie. */
    FIXED_PRICE,

    /** Derive du catalogue par un pourcentage. Un tarif etudiant a moins vingt pour cent. */
    PERCENTAGE_OFF_LIST,

    /** Derive du catalogue par un montant. */
    FIXED_AMOUNT_OFF_LIST
}
