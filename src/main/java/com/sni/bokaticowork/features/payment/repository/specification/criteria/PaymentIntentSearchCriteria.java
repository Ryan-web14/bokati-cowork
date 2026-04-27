package com.sni.bokaticowork.features.payment.repository.specification.criteria;

import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;

public record PaymentIntentSearchCriteria(
        PaymentIntentStatus status,
        String customerType,
        String customerCode,
        String sourceType,
        String sourceCode,
        String searchText
) {
    public String statusValue() {
        return status == null ? null : status.name();
    }
}
