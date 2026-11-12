package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/** Realimentation proposee d'une petite caisse : le total des depenses depuis la derniere. */
public record CashPettyRefillResponse(
        String registerCode,
        String parentRegisterCode,
        BigDecimal amount,
        Instant since,
        Integer expenseCount,
        BigDecimal standardFloat
) {
}
