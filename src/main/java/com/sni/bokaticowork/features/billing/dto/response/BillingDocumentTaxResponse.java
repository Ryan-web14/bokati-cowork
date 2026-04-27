package com.sni.bokaticowork.features.billing.dto.response;

import java.math.BigDecimal;

public record BillingDocumentTaxResponse(
        String taxCode,
        String taxName,
        BigDecimal rate,
        BigDecimal taxableAmount,
        BigDecimal taxAmount
) {
}
