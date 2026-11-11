package com.sni.bokaticowork.features.payment.dto.response;

import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentIntentResponse(
        String intentNumber,
        String customerType,
        String customerCode,
        String customerName,
        String customerEmail,
        String customerPhone,
        String billingAddressJson,
        Boolean customerRegistered,
        BigDecimal amount,
        BigDecimal paidAmount,
        BigDecimal processingAmount,
        BigDecimal remainingAmount,
        BigDecimal payableAmount,
        String currency,
        PaymentIntentStatus status,
        String purpose,
        String sourceType,
        String sourceCode,
        String resolvedSourceType,
        String resolvedSourceCode,
        String resolvedSourceLabel,
        Boolean resolvedSourceRegistered,
        String paymentLinkToken,
        Instant paymentLinkExpiresAt,
        String idempotencyKey,
        Instant expiresAt,
        String metadataJson,
        java.time.Instant cancelledAt,
        String cancelledBy,
        String cancellationReason,
        /** Nombre de relances apres expiration, annulation ou echec. */
        Integer relaunchCount,
        java.time.Instant lastRelaunchedAt,
        String lastRelaunchedBy,
        /** Vrai si l'intention peut etre relancee maintenant (expiree, annulee ou en echec, et pas entierement payee). */
        Boolean relaunchable,
        /** Vrai si l'intention peut etre annulee maintenant (en attente, sans mobile money en cours). */
        Boolean cancellable
) {
}
