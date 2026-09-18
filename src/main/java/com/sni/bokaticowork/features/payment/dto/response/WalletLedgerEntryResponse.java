package com.sni.bokaticowork.features.payment.dto.response;

import com.sni.bokaticowork.features.payment.enums.WalletEntryDirection;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletLedgerEntryResponse(
        String entryNumber,
        /** Reference lisible de la transaction · celle qu'un client cite au telephone. */
        String transactionNumber,
        /** Identifiant technique · celui qu'un systeme tiers rapproche. */
        java.util.UUID transactionUuid,
        String walletNumber,
        WalletEntryDirection direction,
        BigDecimal amount,
        String currency,
        BigDecimal balanceAfter,
        WalletEntryType entryType,
        String sourceType,
        String sourceCode,
        String reference,
        String createdBy,
        Instant createdAt
) {
}
