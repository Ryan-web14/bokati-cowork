package com.sni.bokaticowork.features.payment.dto.response;

import com.sni.bokaticowork.features.payment.enums.WalletHoldStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletHoldResponse(
        String holdNumber,
        String walletNumber,
        BigDecimal amount,
        String currency,
        WalletHoldStatus status,
        String sourceType,
        String sourceCode,
        Instant expiresAt
) {
}
