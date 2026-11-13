package com.sni.bokaticowork.features.billing.dto.response;

import java.util.List;

/**
 * Destinataires d'un client (lot B4) : sa preference enregistree, et les adresses et numeros connus
 * a proposer (adresses de facturation du membre, de l'organisation, de la fiche).
 */
public record BillingRecipientsResponse(
        String customerType,
        String customerCode,
        List<String> to,
        List<String> cc,
        List<String> bcc,
        List<String> whatsappNumbers,
        List<String> channels,
        List<String> suggestedEmails,
        List<String> suggestedPhones
) {
}
