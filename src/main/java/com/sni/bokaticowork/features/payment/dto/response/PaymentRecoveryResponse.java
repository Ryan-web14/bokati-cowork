package com.sni.bokaticowork.features.payment.dto.response;

import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.BillableItemResponse;

import java.math.BigDecimal;
import java.util.List;

public record PaymentRecoveryResponse(
        String customerType,
        String customerCode,
        String currency,
        BigDecimal openDocumentAmount,
        BigDecimal pendingBillableAmount,
        BigDecimal totalPayableAmount,
        List<BillingDocumentResponse> documents,
        List<BillableItemResponse> pendingBillableItems,
        PaymentIntentResponse paymentIntent
) {
}
