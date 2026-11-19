package com.sni.bokaticowork.features.booking.enums;

public enum BookingEventType {
    BOOKING_CREATED,
    BOOKING_HOLD_CREATED,
    BOOKING_HOLD_EXPIRED,
    BOOKING_CONFIRMED,
    BOOKING_APPROVED,
    BOOKING_REJECTED,
    BOOKING_STARTED,
    BOOKING_CHECKED_IN,
    BOOKING_CHECKED_OUT,
    BOOKING_COMPLETED,
    BOOKING_CANCELLED,
    BOOKING_NO_SHOW,
    RESOURCE_RESERVED,
    RESOURCE_RELEASED,
    ENTITLEMENT_RESERVED,
    ENTITLEMENT_CONSUMED,
    ENTITLEMENT_RELEASED,
    USAGE_RECORDED,
    EMAIL_QUEUED,
    EMAIL_SENT,
    EMAIL_FAILED,
    REPAIR_APPLIED,
    QUOTA_OVERRIDE_APPLIED,
    BOOKING_RESCHEDULED,
    /** Lot R1 : prix recalcule apres un changement d'horaire, de duree ou de ressource (D1). */
    BOOKING_PRICE_ADJUSTED,
    BOOKING_TRANSFERRED,
    BOOKING_RESOURCE_CHANGED,
    /** Lot R5d : reglement d'usage accepte. */
    USAGE_RULES_ACCEPTED,
    /** Lot R5d : etat des lieux de fin enregistre. */
    END_REPORT_RECORDED,
    /** Lot B7 : participant ajoute (avant : journalise BOOKING_CREATED). */
    BOOKING_PARTICIPANT_ADDED,
    /** Lot B7 : participant retire. */
    BOOKING_PARTICIPANT_REMOVED,
    /** Lot B3 : rappel au client avant l'echeance de paiement. */
    BOOKING_PAYMENT_REMINDER,
    /** Lot B5 : rendez-vous pris au telephone (note d'appel, prospect relie). */
    BOOKING_PHONE_CALL,
    /** Lot B5 : prospect du CRM converti a la venue ou au paiement. */
    BOOKING_LEAD_CONVERTED,
    /** Lot B9a : le client confirme sa venue depuis le rappel. */
    BOOKING_ATTENDANCE_CONFIRMED,
    /** Lot B9a : creneau libere faute d'arrivee N minutes apres le debut. */
    BOOKING_EARLY_RELEASED,
    /** Lot B9d : invitation envoyee a un participant, reponse, arrivee pointee. */
    BOOKING_PARTICIPANT_INVITED,
    BOOKING_PARTICIPANT_ANSWERED,
    BOOKING_PARTICIPANT_CHECKED_IN
}
