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
        Integer adjustmentValue
) {
}
