package com.sni.bokaticowork.features.payment.dto.response;

import com.sni.bokaticowork.features.payment.enums.WalletStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletResponse(
        String walletNumber,
        String ownerType,
        String ownerCode,
        String currency,
        WalletStatus status,
        BigDecimal availableBalance,
        BigDecimal ledgerBalance,
        BigDecimal heldBalance,
        Instant openedAt,
        Instant closedAt
) {
}
