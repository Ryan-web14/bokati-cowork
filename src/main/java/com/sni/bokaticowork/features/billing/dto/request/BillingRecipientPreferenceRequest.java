package com.sni.bokaticowork.features.billing.dto.request;

import java.util.List;

/** Destinataires et canaux habituels des pieces d'un client (lot B4). */
public record BillingRecipientPreferenceRequest(List<String> to, List<String> cc, List<String> bcc, List<String> whatsappNumbers,
                                                List<String> channels) {
}
