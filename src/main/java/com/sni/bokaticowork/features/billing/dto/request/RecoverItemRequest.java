package com.sni.bokaticowork.features.billing.dto.request;

import java.math.BigDecimal;

public record RecoverItemRequest(
        String notes,
        BigDecimal partialQuantity
) {
}
