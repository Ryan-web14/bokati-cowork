package com.sni.bokaticowork.features.payment.provider;

import java.math.BigDecimal;

public record MobileMoneyRefundRequest(
        String providerReference,
        BigDecimal amount,
        String reason,
        String currency
) {
}
