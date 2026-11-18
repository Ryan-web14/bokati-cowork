package com.sni.bokaticowork.features.ressource.service.support;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lot R2 : reservations a venir d'une ressource, vues depuis le module des ressources. Mis en oeuvre
 * par le module des reservations (qui depend deja des ressources : pas de dependance en boucle).
 */
public interface ResourceBookingImpact {

    /** Une reservation touchee. */
    record ImpactedBooking(String bookingNumber, String status, LocalDateTime startedAt, LocalDateTime endedAt,
                           String ownerType, String ownerCode, String contactName, int quantity) {
    }

    /** Reservations actives (confirmees, en cours, en attente d'approbation ou de paiement) qui chevauchent la plage. */
    List<ImpactedBooking> activeBookings(Long resourceId, LocalDateTime from, LocalDateTime to);

    /** Annule ces reservations et previent les clients (courriel), remboursement selon la politique. */
    void cancelAndNotify(List<String> bookingNumbers, String reason);
}
