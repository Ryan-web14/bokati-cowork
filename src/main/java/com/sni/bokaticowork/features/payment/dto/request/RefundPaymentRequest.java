package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record RefundPaymentRequest(
        BigDecimal amount,
        @NotBlank String reason,
        /** Ignore pour un compte connecte : l'auteur est le compte du jeton. */
        String processedBy,
        /** Remboursement d'especes : session ouverte d'ou sortent les especes (defaut : celle de l'encaissement si encore ouverte). */
        String cashSessionNumber
) {
    public RefundPaymentRequest(BigDecimal amount, String reason, String processedBy) {
        this(amount, reason, processedBy, null);
    }
}
