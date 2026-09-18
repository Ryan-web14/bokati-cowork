package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record CashRegisterResponse(
        String registerCode,
        String name,
        String locationCode,
        String businessEntityCode,
        String deviceCode,
        Boolean active,
        Boolean cashControlEnabled,
        /** Caisse tenue par le systeme · l'interface doit y masquer toute saisie. */
        Boolean systemManaged,
        /** Unique moyen de paiement admis · nul si la caisse les accepte tous. */
        com.sni.bokaticowork.features.payment.enums.PaymentMethod restrictedToMethod,
        BigDecimal maxCashAmount,
        String managerEmail,
        Instant createdAt,
        Instant updatedAt
) {
}
