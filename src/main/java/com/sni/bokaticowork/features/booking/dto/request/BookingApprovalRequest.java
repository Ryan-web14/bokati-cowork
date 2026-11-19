package com.sni.bokaticowork.features.booking.dto.request;

/**
 * Approbation ou refus. Lot B7 : un refus se motive en {@code reasonCode} (liste
 * {@code BOOKING_REJECT_REASON}) plus {@code reasonText} ; {@code reason}, texte libre, reste accepte.
 */
public record BookingApprovalRequest(
        String actor,
        String reason,
        Boolean sendEmail,
        String reasonCode,
        String reasonText
) {
    public BookingApprovalRequest(String actor, String reason, Boolean sendEmail) {
        this(actor, reason, sendEmail, null, null);
    }
}
