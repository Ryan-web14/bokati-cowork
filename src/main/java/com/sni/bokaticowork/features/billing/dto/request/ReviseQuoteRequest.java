package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Revision d'un devis en negociation (lot B2) : les modifications, leur motif et leur origine.
 *
 * @param origin                 CLIENT_PROPOSAL (proposition du client), COUNTER_PROPOSAL
 *                               (contre-proposition) ou INTERNAL ; INTERNAL par defaut
 * @param discountOverrideReason motif de derogation si un prix passe sous le plancher ou une remise
 *                               au-dela du maximum (droit de derogation requis)
 */
public record ReviseQuoteRequest(
        @NotBlank @Size(max = 1000) String reason,
        @Pattern(regexp = "CLIENT_PROPOSAL|COUNTER_PROPOSAL|INTERNAL") String origin,
        @Size(max = 1000) String discountOverrideReason,
        @Valid @NotNull UpdateBillingDocumentRequest changes
) {
}
