package com.sni.bokaticowork.core.accounting;

/**
 * Ce que les modules hors comptabilite demandent sur les creances a risque (lot F19), sans dependre
 * de la comptabilite : une facture contestee ne se relance pas, un client a creance douteuse peut
 * etre limite selon la politique d'acces. Sans implementation (comptabilite absente), rien n'est
 * limite.
 */
public interface ReceivableRiskQuery {

    /** Politique d'acces d'un client a creance douteuse ou litigieuse. */
    enum Policy { NONE, ALERT, PREPAY, BLOCK }

    /** La facture est contestee : ses relances sont suspendues. */
    boolean disputed(String documentNumber);

    /** Politique qui s'applique a ce client, {@code NONE} s'il n'a pas de creance a risque. */
    Policy policyFor(String customerType, String customerCode);

    /**
     * Refuse une nouvelle reservation ou souscription d'un client bloque, sauf derogation du droit
     * {@code CUSTOMER_CREDIT:OVERRIDE} ; renvoie un avertissement a afficher (ou null).
     */
    String guardNewSale(String customerType, String customerCode, String what);
}
