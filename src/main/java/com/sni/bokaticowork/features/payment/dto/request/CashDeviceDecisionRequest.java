package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotNull;

/** Decision sur un poste : l'autoriser ou le refuser, avec un libelle facultatif. */
public record CashDeviceDecisionRequest(
        @NotNull com.sni.bokaticowork.features.payment.enums.CashDeviceStatus status,
        String label
) {
}
