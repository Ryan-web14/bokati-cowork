package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Envoi d'une piece (lot B4). Tout vide : les destinataires et canaux habituels du client (sa
 * preference), a defaut son adresse de facturation par courriel.
 *
 * @param channels         EMAIL (PDF joint) et/ou WHATSAPP (modele avec lien vers l'espace client)
 * @param whatsappNumbers  numeros WhatsApp ; vide : ceux de la preference, a defaut le telephone du client
 */
public record SendDocumentRequest(
        List<String> to,
        List<String> cc,
        List<String> bcc,
        List<String> channels,
        List<String> whatsappNumbers,
        @Size(max = 2000) String message
) {
}
