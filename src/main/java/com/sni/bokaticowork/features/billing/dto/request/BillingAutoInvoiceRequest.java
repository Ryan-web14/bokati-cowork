package com.sni.bokaticowork.features.billing.dto.request;

import com.sni.bokaticowork.features.billing.enums.BillingLineType;

import java.math.BigDecimal;

public record BillingAutoInvoiceRequest(
        String customerType,
        String customerCode,
        String customerName,
        String customerEmail,
        String customerPhone,
        String billingAddressJson,
        String sourceType,
        String sourceCode,
        String title,
        String description,
        String currency,
        BigDecimal amount,
        BillingLineType lineType,
        Boolean taxable,
        String metadataJson
) {
}
