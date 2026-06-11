package com.sni.bokaticowork.features.booking.dto.response;

import java.math.BigDecimal;

public record CancellationPolicyResponse(
        Long id,
        Integer hoursBeforeStart,
        BigDecimal refundPercentage,
        Integer ruleOrder,
        Boolean active
) {
}
