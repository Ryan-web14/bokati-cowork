package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/** Ordre de transfert interne ({@code OTI-}). */
public record CashTransferResponse(
        String transferNumber,
        com.sni.bokaticowork.features.payment.enums.CashTransferKind kind,
        String fromRegister,
        String toRegister,
        String currency,
        BigDecimal sentAmount,
        BigDecimal receivedAmount,
        BigDecimal difference,
        com.sni.bokaticowork.features.payment.enums.CashTransferStatus status,
        String carrier,
        String sentBy,
        Instant sentAt,
        String fromSession,
        String outMovement,
        String receivedBy,
        Instant receivedAt,
        String toSession,
        String inMovement,
        String reviewNumber,
        String cancelledBy,
        Instant cancelledAt,
        String cancelReason,
        Long minutesInTransit,
        String note
) {
}
