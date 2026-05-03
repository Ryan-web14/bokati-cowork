package com.sni.bokaticowork.features.payment.dto.response;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;

import java.math.BigDecimal;

public record PaymentReceiptAllocationResponse(
        String documentNumber,
        BillingDocumentType documentType,
        String title,
        BigDecimal documentTotalAmount,
        BigDecimal allocatedAmount,
        BigDecimal remainingBalanceDue,
        String currency
) {
}
