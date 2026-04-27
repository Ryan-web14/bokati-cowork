package com.sni.bokaticowork.features.billing.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CustomerStatementResponse(
        String customerType,
        String customerCode,
        BigDecimal totalInvoiced,
        BigDecimal totalPaid,
        BigDecimal totalBalanceDue,
        List<BillingDocumentResponse> documents
) {
}
