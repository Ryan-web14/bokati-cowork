package com.sni.bokaticowork.features.billing.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Lot C8 : requetes et reponses des tarifs negocies par client. */
public final class CustomerPriceDtos {

    private CustomerPriceDtos() {
    }

    /**
     * Nouveau tarif : prix fixe ({@code unitPrice}) ou taux de remise ({@code discountRate}), l'un
     * des deux. Le tarif en cours du meme article s'arrete la veille de {@code validFrom}.
     */
    public record PriceRequest(@NotBlank String itemCode, @DecimalMin("0") BigDecimal unitPrice,
                               @DecimalMin("0") @DecimalMax("100") BigDecimal discountRate, @NotNull LocalDate validFrom, LocalDate validUntil,
                               @Size(max = 120) String contractReference, @Size(max = 500) String reason) {
    }

    /** Fin d'un tarif a une date. */
    public record EndRequest(@NotNull LocalDate validUntil) {
    }

    public record PriceView(Long id, String customerType, String customerCode, String itemCode, String itemName, BigDecimal unitPrice,
                            BigDecimal discountRate, String currency, LocalDate validFrom, LocalDate validUntil, boolean currentlyValid,
                            String contractReference, String reason, String createdBy, Instant createdAt) {
    }
}
