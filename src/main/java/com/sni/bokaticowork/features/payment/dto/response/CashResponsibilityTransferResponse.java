package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Transfert de responsabilite (PV de remise de caisse). */
public record CashResponsibilityTransferResponse(
        String transferNumber,
        String registerCode,
        String fromUser,
        String toUser,
        String reasonCode,
        String reasonLabel,
        String reasonText,
        Boolean permanent,
        Instant effectiveAt,
        Instant endsAt,
        BigDecimal expectedAmount,
        BigDecimal countedAmount,
        BigDecimal difference,
        String witness,
        List<CashPendingItemResponse> pendingItems,
        com.sni.bokaticowork.features.payment.enums.CashResponsibilityTransferStatus status,
        String outgoingSignedBy,
        Instant outgoingSignedAt,
        String incomingSignedBy,
        Instant incomingSignedAt,
        String createdBy,
        Instant createdAt
) {
}
