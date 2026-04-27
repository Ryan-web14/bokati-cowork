package com.sni.bokaticowork.features.billing.dto.response;

public record BillingDocumentClauseResponse(
        String clauseCode,
        String title,
        String body,
        Integer displayOrder
) {
}
