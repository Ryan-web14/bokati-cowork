package com.sni.bokaticowork.features.subscription.subscription.dto.response;

import java.math.BigDecimal;

public record EntitlementOperationResponse(
        boolean allowed,
        String entitlementCode,
        BigDecimal requestedQuantity,
        BigDecimal availableQuantity,
        String message
) {
}
