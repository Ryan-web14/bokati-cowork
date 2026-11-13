package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Nouveau prix d'un article pour un segment, a partir d'une date (lot B10) ; vide : tous, des aujourd'hui. */
public record CatalogPriceRequest(@Pattern(regexp = "ALL|MEMBER|COMPANY|VISITOR") String segment, @NotNull @PositiveOrZero BigDecimal unitPrice,
                                  LocalDate validFrom, @Size(max = 500) String reason) {
}
