package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Changement de client sur une facture (lot B6).
 *
 * @param payments sort des reglements deja recus (facture scellee, comptabilisee ou payee) :
 *                 KEEP_AS_CREDIT (avoir au client d'origine, a rembourser ou imputer) ou TRANSFER
 *                 (reportes sur la nouvelle facture, le payeur reel reste trace) ; KEEP_AS_CREDIT par defaut
 */
public record ChangeCustomerRequest(
        @NotBlank String customerType,
        @NotBlank String customerCode,
        @NotBlank @Size(max = 1000) String reason,
        @Pattern(regexp = "KEEP_AS_CREDIT|TRANSFER") String payments
) {
}
