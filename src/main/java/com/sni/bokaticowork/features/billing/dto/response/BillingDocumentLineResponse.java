package com.sni.bokaticowork.features.billing.dto.response;

import com.sni.bokaticowork.features.billing.enums.BillingLineType;

import java.math.BigDecimal;

public record BillingDocumentLineResponse(
        Integer lineOrder,
        BillingLineType lineType,
        String itemCode,
        String description,
        String detailedDescription,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountRate,
        BigDecimal discountAmount,
        Boolean taxable,
        BigDecimal vatRate,
        BigDecimal additionalCentRate,
        BigDecimal subtotalAmount,
        BigDecimal taxableAmount,
        BigDecimal vatAmount,
        BigDecimal additionalCentAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String sourceType,
        String sourceCode
) {
}
