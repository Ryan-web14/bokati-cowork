package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * {@code createdBy} est accepte et ignore · le front l'envoie, mais l'auteur d'une intention est
 * celui du jeton, trace par l'audit. Un auteur ecrit par le client ne prouverait rien. Les autres
 * proprietes inconnues restent refusees.
 */
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(value = {"createdBy"})
public record CreatePaymentIntentRequest(
        @NotBlank String customerType,
        @NotBlank String customerCode,
        @NotNull BigDecimal amount,
        @NotBlank String currency,
        String purpose,
        String sourceType,
        String sourceCode,
        String idempotencyKey,
        Instant expiresAt,
        String metadataJson
) {
}
