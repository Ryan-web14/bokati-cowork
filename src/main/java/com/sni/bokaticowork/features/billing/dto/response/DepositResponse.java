package com.sni.bokaticowork.features.billing.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Acompte et sa facture d'acompte (lot B3). */
public record DepositResponse(
        Long id,
        String documentNumber,
        String finalDocumentNumber,
        Integer rank,
        String label,
        String mode,
        BigDecimal value,
        String baseLineOrders,
        BigDecimal amount,
        LocalDate dueDate,
        String status,
        String depositInvoiceNumber,
        BigDecimal paidAmount,
        BigDecimal deductedAmount,
        String deductedOn,
        Instant deductedAt,
        String settlementDocument,
        String cancelReason,
        Instant createdAt
) {
}
