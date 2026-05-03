package com.sni.bokaticowork.features.payment.provider;

import java.math.BigDecimal;

public record MobileMoneyInitiationRequest(
        String intentNumber,
        String customerCode,
        String phoneNumber,
        BigDecimal amount,
        String currency,
        String callbackUrl,
        String correspondent
) {
}
