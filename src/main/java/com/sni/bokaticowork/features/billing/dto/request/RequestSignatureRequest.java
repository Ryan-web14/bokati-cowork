package com.sni.bokaticowork.features.billing.dto.request;

/**
 * Demande d'envoi d'un lien de signature électronique au client.
 * Si signerName/signerEmail sont null, les données du document sont utilisées.
 */
public record RequestSignatureRequest(
        String signerName,
        String signerEmail,
        String customMessage
) {
}
