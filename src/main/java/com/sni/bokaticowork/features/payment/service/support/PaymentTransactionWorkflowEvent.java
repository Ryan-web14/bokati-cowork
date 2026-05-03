package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;

public record PaymentTransactionWorkflowEvent(
        String transactionNumber,
        PaymentTransactionStatus status
) {
}
