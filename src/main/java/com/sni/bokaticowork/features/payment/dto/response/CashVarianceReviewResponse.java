package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/** Un ecart en revue ou decide. */
public record CashVarianceReviewResponse(
        String reviewNumber,
        String registerCode,
        String sessionNumber,
        com.sni.bokaticowork.features.payment.enums.CashVarianceKind kind,
        BigDecimal amount,
        String countNumber,
        String cashier,
        String reason,
        com.sni.bokaticowork.features.payment.enums.CashVarianceStatus status,
        com.sni.bokaticowork.features.payment.enums.CashVarianceDecision decision,
        com.sni.bokaticowork.features.payment.enums.CashVarianceDecision proposedDecision,
        String decidedBy,
        Instant decidedAt,
        String decisionNote,
        Instant contestDeadline,
        Instant contestedAt,
        String contestReason,
        Instant reviewDueAt,
        Boolean decidableNow,
        Instant createdAt
) {
}
