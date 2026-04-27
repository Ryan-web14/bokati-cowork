package com.sni.bokaticowork.features.billing.dto.response;

import com.sni.bokaticowork.features.billing.enums.PaymentScheduleStatus;

import java.math.BigDecimal;
import java.util.List;

public record PaymentScheduleResponse(
        String scheduleNumber,
        String billingDocumentNumber,
        PaymentScheduleStatus status,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal balanceDue,
        String currency,
        String notes,
        List<PaymentScheduleInstallmentResponse> installments
) {
}