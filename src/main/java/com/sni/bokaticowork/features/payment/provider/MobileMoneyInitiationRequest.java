package com.sni.bokaticowork.features.payment.provider;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record MobileMoneyInitiationRequest(
        String depositId,
        String intentNumber,
        String transactionNumber,
        String customerCode,
        String phoneNumber,
        BigDecimal amount,
        String currency,
        String callbackUrl,
        String correspondent,
        String clientReferenceId,
        String customerMessage,
        List<Map<String, Object>> metadata
) {
}
