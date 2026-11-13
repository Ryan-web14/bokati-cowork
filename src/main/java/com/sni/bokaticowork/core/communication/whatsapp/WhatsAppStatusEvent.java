package com.sni.bokaticowork.core.communication.whatsapp;

import java.time.Instant;

/**
 * Etat d'un message WhatsApp : pris en charge a l'envoi, puis remis, lu ou en echec selon Infobip.
 * Le module de notifications le reprend dans notification_message (canal WHATSAPP).
 *
 * @param statusGroup PENDING, DELIVERED, UNDELIVERABLE, REJECTED, EXPIRED (groupes d'Infobip), ou SEEN
 */
public record WhatsAppStatusEvent(String messageId, String notificationNumber, String phone, String purpose, String templateName, String reference,
                                  String statusGroup, String statusName, String description, String error, Instant sentAt, Instant doneAt,
                                  Instant seenAt) {

    /** Le message n'arrivera pas. */
    public boolean failed() {
        return error != null || "UNDELIVERABLE".equals(statusGroup) || "REJECTED".equals(statusGroup) || "EXPIRED".equals(statusGroup);
    }
}
