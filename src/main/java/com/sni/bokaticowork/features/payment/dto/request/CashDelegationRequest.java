package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Delegation d'un reviseur ou superviseur absent. Elle ne donne aucun droit : le delegue doit deja
 * avoir les permissions deleguees ; elle redirige les revues et les notifications.
 */
public record CashDelegationRequest(
        @NotBlank String delegateEmail,
        @NotEmpty List<String> permissions,
        @NotNull LocalDate startsOn,
        @NotNull LocalDate endsOn,
        String note
) {
}
