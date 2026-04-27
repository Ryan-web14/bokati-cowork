package com.sni.bokaticowork.features.billing.dto.response;

import com.sni.bokaticowork.features.billing.enums.PaymentScheduleInstallmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PaymentScheduleInstallmentResponse(
        String installmentNumber,
        int installmentOrder,
        String label,
        BigDecimal amount,
        LocalDate dueDate,
        BigDecimal paidAmount,
        BigDecimal balanceDue,
        PaymentScheduleInstallmentStatus status,
        Instant paidAt
) {
}