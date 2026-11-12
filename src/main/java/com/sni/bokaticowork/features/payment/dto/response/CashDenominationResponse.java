package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CashDenominationResponse(
        String currency,
        BigDecimal value,
        com.sni.bokaticowork.features.payment.enums.CashDenominationKind kind,
        String label,
        Integer sortOrder,
        LocalDate activeFrom,
        LocalDate activeTo,
        Boolean active
) {
}
