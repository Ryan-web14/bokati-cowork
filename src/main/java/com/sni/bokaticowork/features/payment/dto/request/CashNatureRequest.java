package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Une nature d'operation, dans la liste complete envoyee par {@code PUT /cash-registers/natures}. */
public record CashNatureRequest(
        @NotBlank String code,
        @NotBlank String label,
        @NotNull com.sni.bokaticowork.features.payment.enums.CashNatureFamily family,
        @NotNull com.sni.bokaticowork.features.payment.enums.CashFlowDirection direction,
        com.sni.bokaticowork.features.payment.enums.CashMovementType movementType,
        String accountNumber,
        String vatCode,
        Boolean documentRequired,
        Integer justificationDays,
        String defaultSection,
        BigDecimal maxPerOperation,
        Boolean priorRequestRequired,
        BigDecimal doubleVisaThreshold,
        String keywords,
        Integer sortOrder,
        Boolean active
) {
}
