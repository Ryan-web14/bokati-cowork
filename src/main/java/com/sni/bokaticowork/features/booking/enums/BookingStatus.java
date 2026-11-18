package com.sni.bokaticowork.features.booking.enums;

public enum BookingStatus {
    DRAFT,
    PENDING_APPROVAL,
    PENDING_PAYMENT,
    REJECTED,
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    NO_SHOW,
    EXPIRED;

    /** Lot B8 : libelle francais, accorde au feminin (« Une réservation confirmée »). */
    public String label() {
        return switch (this) {
            case DRAFT -> "en brouillon";
            case PENDING_APPROVAL -> "en attente d'approbation";
            case PENDING_PAYMENT -> "en attente de paiement";
            case REJECTED -> "refusée";
            case CONFIRMED -> "confirmée";
            case IN_PROGRESS -> "en cours";
            case COMPLETED -> "terminée";
            case CANCELLED -> "annulée";
            case NO_SHOW -> "marquée absente";
            case EXPIRED -> "expirée";
        };
    }
}
