package com.sni.bokaticowork.core.enums;

public enum EventType {
    BOOKING_CONFIRMED, BOOKING_CANCELLED, BOOKING_REMINDER, BOOKING_FAILED,
    INVOICE_ISSUED, PAYMENT_RECEIVED, PAYMENT_FAILED,
    /** Lot KG7 : pieces KYC demandees (WhatsApp ; le membre peut le couper). */
    KYC_DOCUMENTS_REQUESTED,
    /** Lot B9f : offres de derniere minute (WhatsApp) ; le membre doit les accepter, coupees par defaut. */
    LAST_MINUTE_OFFERS;

    /** Une preference sans choix du membre vaut-elle accord ? Oui, sauf pour la prospection. */
    public boolean defaultEnabled() {
        return this != LAST_MINUTE_OFFERS;
    }
}
