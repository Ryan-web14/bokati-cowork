package com.sni.bokaticowork.features.billing.dto.response;

import com.sni.bokaticowork.features.billing.enums.BillingAdvanceStatus;
import com.sni.bokaticowork.features.billing.enums.BillingAdvanceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BillingDocumentAdvanceResponse(
        BillingAdvanceType advanceType,
        BigDecimal advanceValue,
        BigDecimal computedAmount,
        List<Integer> includedLineOrders,
        List<Integer> excludedLineOrders,
        String paymentReference,
        String referenceLabel,
        LocalDate dueDate,
        BillingAdvanceStatus status,
        Instant paidAt,
        String notes
) {
}