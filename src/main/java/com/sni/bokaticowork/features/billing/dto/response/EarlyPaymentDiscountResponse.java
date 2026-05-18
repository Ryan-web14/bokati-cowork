package com.sni.bokaticowork.features.billing.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EarlyPaymentDiscountResponse(
        BigDecimal discountRate,
        LocalDate ifPaidBefore,
        BigDecimal computedAmount,
        String label
) {
}
