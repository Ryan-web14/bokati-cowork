package com.sni.bokaticowork.features.billing.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Fiche d'affaire (lot B11). */
public record DealResponse(
        String code,
        String customerType,
        String customerCode,
        String customerName,
        String title,
        String owner,
        String ownerName,
        BigDecimal expectedAmount,
        String currency,
        Integer probability,
        String stage,
        String lostReason,
        LocalDate expectedCloseDate,
        Instant closedAt,
        String source,
        String crmOpportunityRef,
        String analyticSection,
        String notes,
        String createdBy,
        Instant createdAt
) {
}
