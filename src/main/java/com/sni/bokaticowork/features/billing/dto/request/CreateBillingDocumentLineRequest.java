package com.sni.bokaticowork.features.billing.dto.request;

import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateBillingDocumentLineRequest(
        Integer lineOrder,
        BillingLineType lineType,
        String itemCode,
        @NotBlank String description,
        String detailedDescription,
        BigDecimal quantity,
        @NotNull BigDecimal unitPrice,
        BigDecimal discountRate,
        BigDecimal discountAmount,
        Boolean taxable,
        BigDecimal vatRate,
        BigDecimal additionalCentRate,
        String sourceType,
        String sourceCode,
        String unit,
        String externalReference,
        String notes,
        Boolean optional
) {
}
