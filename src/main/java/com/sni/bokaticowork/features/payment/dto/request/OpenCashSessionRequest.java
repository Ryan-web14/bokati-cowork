package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record OpenCashSessionRequest(
        @NotBlank String registerCode,
        /** Ignore en contexte authentifie : l'auteur est l'utilisateur connecte. */
        String openedBy,
        BigDecimal openingAmount
) {
}
