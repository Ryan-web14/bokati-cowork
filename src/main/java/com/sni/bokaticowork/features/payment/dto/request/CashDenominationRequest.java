package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Une coupure de la liste complete d'une devise ; une coupure absente est retiree (date de fin). */
public record CashDenominationRequest(
        @NotNull BigDecimal value,
        @NotNull com.sni.bokaticowork.features.payment.enums.CashDenominationKind kind,
        @NotBlank String label,
        Integer sortOrder,
        LocalDate activeFrom,
        LocalDate activeTo
) {
}
