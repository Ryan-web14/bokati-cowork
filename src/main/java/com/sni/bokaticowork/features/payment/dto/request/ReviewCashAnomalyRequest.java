package com.sni.bokaticowork.features.payment.dto.request;

import com.sni.bokaticowork.features.payment.enums.CashAnomalyStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewCashAnomalyRequest(
        /** Ignore en contexte authentifie : l'auteur est l'utilisateur connecte. */
        String reviewedBy,
        @NotNull CashAnomalyStatus status,
        String note
) {
}
