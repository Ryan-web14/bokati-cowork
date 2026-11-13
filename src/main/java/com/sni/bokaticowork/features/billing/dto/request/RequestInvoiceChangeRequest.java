package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Demande de modification d'une facture non payee (lot B14) : les modifications (meme forme que la
 * mise a jour d'un document) et leur motif.
 *
 * @param discountOverrideReason motif de derogation si la nouvelle version passe sous un plancher
 */
public record RequestInvoiceChangeRequest(
        @NotBlank @Size(max = 1000) String reason,
        @Size(max = 1000) String discountOverrideReason,
        @Valid @NotNull UpdateBillingDocumentRequest changes
) {
}
