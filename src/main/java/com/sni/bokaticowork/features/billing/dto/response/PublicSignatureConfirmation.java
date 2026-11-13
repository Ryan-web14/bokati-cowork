package com.sni.bokaticowork.features.billing.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Reponse publique a la signature d'un devis, pour quiconque detient le jeton : seulement ce que le
 * signataire a vu et signe. Jamais de notes internes, d'objets a recuperer ni de lignes.
 */
public record PublicSignatureConfirmation(
        String quoteNumber,
        LocalDate issueDate,
        BigDecimal totalAmount,
        String currency,
        String signatureStatus,
        String signerName,
        Instant signedAt,
        Integer revision
) {
}
