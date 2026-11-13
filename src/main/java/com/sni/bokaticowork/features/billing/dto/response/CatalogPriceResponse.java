package com.sni.bokaticowork.features.billing.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Prix d'un article (lot B10) ; {@code source} : SEGMENT, ALL (prix date commun) ou CATALOG (prix de la fiche). */
public record CatalogPriceResponse(String itemCode, String segment, BigDecimal unitPrice, LocalDate validFrom, LocalDate validUntil, String reason,
                                   String createdBy, Instant createdAt, String source) {
}
