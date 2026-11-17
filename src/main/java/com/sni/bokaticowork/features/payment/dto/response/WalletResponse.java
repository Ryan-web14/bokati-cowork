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
        Instant closedAt,
        /** Nom du titulaire (membre, client, entreprise). */
        String ownerName,
        /** Retenues en cours : ce que bloque chaque montant de {@code heldBalance}. */
        java.util.List<HoldLine> activeHolds
) {
    public WalletResponse(String walletNumber, String ownerType, String ownerCode, String currency, WalletStatus status,
                          BigDecimal availableBalance, BigDecimal ledgerBalance, BigDecimal heldBalance,
                          Instant openedAt, Instant closedAt) {
        this(walletNumber, ownerType, ownerCode, currency, status, availableBalance, ledgerBalance, heldBalance,
                openedAt, closedAt, null, java.util.List.of());
    }

    /** Une retenue en cours et ce qu'elle concerne (reservation, abonnement...). */
    public record HoldLine(String holdNumber, BigDecimal amount, String sourceType, String sourceCode,
                           String sourceLabel, Instant expiresAt, Instant createdAt) {
    }
}
