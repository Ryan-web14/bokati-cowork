package com.sni.bokaticowork.features.billing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Lot C13 : lancements de facturation groupee. */
public final class BatchBillingDtos {

    private BatchBillingDtos() {
    }

    /**
     * Apercu d'une periode. {@code sourceTypes} limite aux consommations de ces origines (par exemple
     * BOOKING, SUBSCRIPTION, OVERAGE) ; vide : toutes. {@code title} : titre des factures.
     */
    public record PreviewRequest(@NotNull LocalDate periodFrom, @NotNull LocalDate periodTo, List<String> sourceTypes,
                                 @Size(max = 255) String title) {
    }

    public record ExcludeRequest(@NotNull Long itemId, @NotNull Boolean excluded) {
    }

    public record RunItem(Long itemId, String customerType, String customerCode, String customerName, String billingCompany, Integer itemCount,
                          BigDecimal totalAmount, String currency, boolean excluded, String invoiceNumber, String error) {
    }

    public record RunView(String runNumber, LocalDate periodFrom, LocalDate periodTo, String title, String status, String createdBy,
                          String issuedBy, Instant issuedAt, BigDecimal totalToInvoice, List<RunItem> customers) {
    }

    public record RunSummary(String runNumber, LocalDate periodFrom, LocalDate periodTo, String title, String status, int customers,
                             long invoices, Instant createdAt) {
    }
}
