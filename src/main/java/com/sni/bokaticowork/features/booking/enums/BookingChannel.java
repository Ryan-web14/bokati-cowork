package com.sni.bokaticowork.features.booking.enums;

/** Lot B0 : canal par lequel la reservation a ete prise. */
public enum BookingChannel {
    /** Espace client (portail). */
    PORTAL,
    /** Back-office, sans precision. */
    TEAM,
    /** Prise au telephone par l'equipe. */
    PHONE,
    /** A l'accueil, client present. */
    DESK,
    /** Demande recue par courriel. */
    EMAIL,
    /** Demande recue par WhatsApp. */
    WHATSAPP
}
