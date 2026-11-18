package com.sni.bokaticowork.features.booking.enums;

public enum BookingLineType {
    RESOURCE,
    ENTITLEMENT,
    BILLABLE,
    /** Lot R5c : service additionnel du catalogue. */
    SERVICE,
    /** Lot R5c : equipement optionnel de la ressource. */
    AMENITY,
    /** Lot R5c : participants supplementaires. */
    PARTICIPANT,
    /** Lot R5c : caution. */
    DEPOSIT,
    /** Lot R5c : depassement au depart. */
    OVERRUN,
    /** Lot R5c : frais de non-presentation. */
    NO_SHOW,
    /** Lot R5c : remise (code promo, lot de ressources), montant negatif. */
    DISCOUNT
}
