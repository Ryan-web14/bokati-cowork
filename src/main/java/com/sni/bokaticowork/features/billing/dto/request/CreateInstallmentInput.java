package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateInstallmentInput(
        String label,
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate dueDate
) {
}