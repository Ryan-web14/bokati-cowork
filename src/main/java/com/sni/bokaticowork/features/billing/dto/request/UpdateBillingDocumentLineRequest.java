package com.sni.bokaticowork.features.billing.dto.request;

import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateBillingDocumentLineRequest(
        /** UPSERT crée ou met à jour la ligne, REMOVE la supprime. */
        @NotNull String action,
        @NotNull Integer lineOrder,
        BillingLineType lineType,
        String itemCode,
        @NotBlank String description,
        String detailedDescription,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountRate,
        BigDecimal discountAmount,
        Boolean taxable,
        Boolean taxIncluded,
        BigDecimal vatRate,
        BigDecimal additionalCentRate,
        String unit,
        String sourceType,
        String sourceCode,
        String externalReference,
        String notes,
        Boolean optional,
        /** Voir {@link CreateBillingDocumentLineRequest#category()}. */
        String category
) {
}
