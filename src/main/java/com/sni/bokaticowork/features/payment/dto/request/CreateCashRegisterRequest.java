package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateCashRegisterRequest(
        @NotBlank String name,
        String locationCode,
        String businessEntityCode,
        String deviceCode,
        Boolean cashControlEnabled,
        BigDecimal maxCashAmount,
        String managerEmail
) {
}
