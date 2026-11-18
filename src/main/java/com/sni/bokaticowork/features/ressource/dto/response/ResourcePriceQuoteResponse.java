package com.sni.bokaticowork.features.ressource.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ResourcePriceQuoteResponse(
        String resourceCode,
        String bookingUnit,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer basePrice,
        Integer finalPrice,
        Long appliedRuleId,
        String appliedRuleLabel,
        String adjustmentType,
        Integer adjustmentValue,
        /** Lot R1 : nombre d'unites (heures fractionnees ou forfaits). */
        java.math.BigDecimal units,
        /** Lot R1 : quantite facturee (postes, ou participants pour une regle par participant). */
        Integer quantity,
        /** Lot R1 : montant exact, le meme que celui de la reservation. */
        java.math.BigDecimal amount,
        String currency,
        /** Lot R1 : tranches (heures pleines et creuses), regles et ajustements appliques. */
        java.util.List<com.sni.bokaticowork.features.ressource.service.pricing.PriceResult.Line> lines,
        /** Lot R1 : cout de chaque unite possible ; l'unite retenue est la moins chere. */
        java.util.List<com.sni.bokaticowork.features.ressource.service.pricing.PriceResult.Alternative> alternatives
) {
}
