package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/** Passation (PV PVP). */
public record CashHandoverResponse(
        String handoverNumber,
        String registerCode,
        String fromSession,
        String toSession,
        String outgoing,
        String incoming,
        String outgoingCount,
        String incomingCount,
        BigDecimal outgoingAmount,
        BigDecimal incomingAmount,
        com.sni.bokaticowork.features.payment.enums.CashHandoverStatus status,
        String disputeReason,
        Instant outgoingSignedAt,
        Instant incomingSignedAt,
        Instant createdAt
) {
}
