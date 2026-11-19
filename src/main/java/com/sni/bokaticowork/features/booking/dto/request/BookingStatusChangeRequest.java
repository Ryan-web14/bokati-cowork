package com.sni.bokaticowork.features.booking.dto.request;

/**
 * Changement de statut (confirmer, demarrer, terminer, annuler, absence).
 *
 * <p>Lot B7 : un motif d'annulation se donne en {@code reasonCode} (liste {@code BOOKING_CANCEL_REASON})
 * plus {@code reasonText} (precision) ; {@code reason}, texte libre, reste accepte. {@code changedBy}
 * n'est lu que hors session (traitement interne).</p>
 */
public record BookingStatusChangeRequest(
        String changedBy,
        String reason,
        Boolean sendEmail,
        String reasonCode,
        String reasonText
) {
    public BookingStatusChangeRequest(String changedBy, String reason, Boolean sendEmail) {
        this(changedBy, reason, sendEmail, null, null);
    }
}
