package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/** Une affectation dans la liste complete envoyee par {@code PUT /cash-registers/{code}/assignments}. */
public record CashRegisterAssignmentRequest(
        @NotBlank String userEmail,
        @NotNull com.sni.bokaticowork.features.payment.enums.CashAssignmentRole role,
        @NotNull com.sni.bokaticowork.features.payment.enums.CashAssignmentQuality quality,
        Integer backupRank,
        Instant validFrom,
        Instant validTo,
        String note
) {
}
