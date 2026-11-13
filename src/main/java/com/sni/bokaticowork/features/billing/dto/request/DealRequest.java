package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fiche d'affaire (lot B11). Le client est un client connu (type et code) ou, pour un prospect, un
 * simple nom ; le responsable est l'utilisateur connecte s'il n'est pas precise.
 */
public record DealRequest(
        String customerType,
        String customerCode,
        @Size(max = 255) String customerName,
        @NotBlank @Size(max = 500) String title,
        @Size(max = 180) String owner,
        BigDecimal expectedAmount,
        @Size(max = 3) String currency,
        @Min(0) @Max(100) Integer probability,
        LocalDate expectedCloseDate,
        @Size(max = 60) String analyticSection,
        @Size(max = 4000) String notes
) {
}
