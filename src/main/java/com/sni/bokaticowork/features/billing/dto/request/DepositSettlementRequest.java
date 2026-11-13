package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Sort d'un acompte encaisse sur un devis refuse, expire ou annule (decision D4) : rembourse au
 * portefeuille du client (REFUND) ou garde en avoir (CREDIT), avec motif.
 */
public record DepositSettlementRequest(@NotBlank @Pattern(regexp = "REFUND|CREDIT") String mode, @NotBlank @Size(max = 1000) String reason) {
}
