package com.sni.bokaticowork.features.billing.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Envoi d'une piece (lot B4).
 *
 * @param deliveredAt WhatsApp : remise au telephone ; @param seenAt WhatsApp : lecture
 */
public record BillingSendingResponse(
        Long id,
        String documentNumber,
        String channel,
        List<String> to,
        List<String> cc,
        List<String> bcc,
        String subject,
        String message,
        boolean duplicate,
        String status,
        String providerReference,
        String error,
        String sentBy,
        String sentByName,
        Instant sentAt,
        Instant deliveredAt,
        Instant seenAt
) {
}
