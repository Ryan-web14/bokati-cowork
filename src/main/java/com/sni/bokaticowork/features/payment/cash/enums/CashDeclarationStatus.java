package com.sni.bokaticowork.features.payment.cash.enums;

/**
 * Ou en est un encaissement en especes annonce depuis l espace client.
 *
 * <p>Annoncer n est pas payer · l argent n arrive que lorsque la personne se presente. Tant que la
 * caisse ne l a pas compte, rien n est encaisse et la facture reste due.</p>
 */
public enum CashDeclarationStatus {

    /** Le client a annonce qu il paierait en especes · la caisse attend. */
    AWAITING_CONFIRMATION,

    /** La caisse a compte l argent · un paiement en especes a ete enregistre. */
    CONFIRMED,

    /** Annulee · par le client qui se ravise, ou par la caisse qui fait le menage. */
    CANCELLED,

    /** Le delai est passe sans que personne ne se presente. */
    EXPIRED;

    /** L annonce attend encore l argent · c est le seul etat ou la caisse a quelque chose a faire. */
    public boolean open() {
        return this == AWAITING_CONFIRMATION;
    }
}
