package com.sni.bokaticowork.features.payment.enums;

/**
 * Qui a declenche l encaissement · le client lui-meme, ou quelqu un de la maison.
 *
 * <p>La distinction ne sert pas a compter : elle sert a prevenir. Un paiement saisi au guichet est
 * deja connu de la personne qui l a saisi. Un paiement fait depuis l espace client, lui, n est
 * annonce a personne · la caisse apprend le reglement en consultant la facture, parfois des jours
 * plus tard, parfois jamais.</p>
 */
public enum PaymentChannel {

    /** Le client a paye depuis son espace, sans personne en face. */
    SELF_SERVICE,

    /** Un agent a enregistre le paiement · caisse, portail d administration. */
    BACK_OFFICE,

    /** Ni l un ni l autre · un worker, un rappel d operateur, une reprise automatique. */
    SYSTEM
}
