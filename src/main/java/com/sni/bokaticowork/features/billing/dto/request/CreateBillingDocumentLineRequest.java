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
        Boolean taxIncluded,
        BigDecimal vatRate,
        BigDecimal additionalCentRate,
        String sourceType,
        String sourceCode,
        String unit,
        String externalReference,
        String notes,
        Boolean optional,
        /**
         * Categorie affichee sur le document. Laisser null pour reprendre automatiquement celle
         * de l'article du catalogue designe par {@code itemCode}.
         */
        String category
) {
}
