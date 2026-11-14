package com.sni.bokaticowork.features.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Lot C9 : paiements non affectes et leur affectation. */
public final class UnallocatedPaymentDtos {

    private UnallocatedPaymentDtos() {
    }

    /** Facture candidate : note (sur 100) et raisons, montant propose. */
    public record Candidate(String documentNumber, String title, BigDecimal balanceDue, LocalDate dueDate, int score, List<String> reasons,
                            BigDecimal proposedAmount) {
    }

    /** Somme en attente sur un paiement. */
    public record Suggestion(String transactionNumber, String customerType, String customerCode, BigDecimal amount, BigDecimal pending,
                             String currency, Instant paidAt, String providerReference, String payerName, List<Candidate> candidates) {
    }

    public record Allocation(@NotBlank String documentNumber, @NotNull BigDecimal amount) {
    }

    /**
     * Affectation a un element (lot R3) : ses factures ouvertes recoivent le montant, echeance la plus
     * ancienne d'abord. {@code activationMode} : {@code FULL_SETTLEMENT} (au solde, par defaut) ou
     * {@code FIRST_PAYMENT} (des le premier paiement), selon ce qui a ete convenu avec le client.
     */
    public record ElementAllocation(@NotBlank String type, @NotBlank String code, @NotNull BigDecimal amount, String activationMode) {
    }

    /** Factures designees par leur numero, elements designes par leur type et leur code, ou les deux. */
    public record AllocateRequest(List<Allocation> allocations, List<ElementAllocation> elements) {
    }

    public record AllocationResult(String transactionNumber, BigDecimal allocated, BigDecimal stillPending, List<String> documentNumbers) {
    }
}
