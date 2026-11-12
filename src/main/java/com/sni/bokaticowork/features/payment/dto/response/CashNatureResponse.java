package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;

public record CashNatureResponse(
        String code,
        String label,
        com.sni.bokaticowork.features.payment.enums.CashNatureFamily family,
        com.sni.bokaticowork.features.payment.enums.CashFlowDirection direction,
        com.sni.bokaticowork.features.payment.enums.CashMovementType movementType,
        String accountNumber,
        String vatCode,
        Boolean documentRequired,
        Integer justificationDays,
        String defaultSection,
        BigDecimal maxPerOperation,
        Boolean priorRequestRequired,
        BigDecimal doubleVisaThreshold,
        String keywords,
        Integer sortOrder,
        Long usageCount,
        Boolean active,
        Boolean systemValue
) {
}
