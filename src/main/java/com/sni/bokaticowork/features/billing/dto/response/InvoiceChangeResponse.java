package com.sni.bokaticowork.features.billing.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Demande de modification d'une facture (lot B14).
 *
 * @param approvalNumber      demande d'approbation du centre de la facturation
 * @param result              NEW_VERSION ou CORRECTIVE_INVOICE une fois appliquee
 * @param resultDocumentNumber facture modifiee, ou rectificative qui la remplace
 */
public record InvoiceChangeResponse(
        Long id,
        String invoiceNumber,
        String status,
        String reason,
        String requestedBy,
        String requestedByName,
        Instant requestedAt,
        BigDecimal totalBefore,
        BigDecimal totalAfter,
        String approvalNumber,
        Instant decidedAt,
        String decidedBy,
        String decisionComment,
        String result,
        String resultDocumentNumber,
        Integer versionNumber
) {
}
