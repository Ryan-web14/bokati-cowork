package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateBillingDocumentClauseRequest(
        String clauseCode,
        @NotBlank String title,
        @NotBlank String body,
        Integer displayOrder
) {
}
