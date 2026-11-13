package com.sni.bokaticowork.features.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Lot C12 : requetes et reponses du recouvrement. */
public final class CollectionDtos {

    private CollectionDtos() {
    }

    /** Promesse : date, montant (par defaut le reste du), canal (PHONE, EMAIL, WHATSAPP, VISIT, PORTAL), note. */
    public record PromiseRequest(@NotNull LocalDate promisedDate, @DecimalMin("0.01") BigDecimal amount, String channel,
                                 @Size(max = 1000) String note) {
    }

    /**
     * Plan de reglement : le reste du se repartit en {@code installments} echeances egales, la
     * premiere le {@code firstDueDate}, puis tous les {@code intervalMonths} mois.
     */
    public record PlanRequest(@NotNull @Min(2) @Max(36) Integer installments, @NotNull LocalDate firstDueDate,
                              @Min(1) @Max(12) Integer intervalMonths, @Size(max = 1000) String notes) {
    }

    public record PromiseView(String promiseNumber, String documentNumber, String customerType, String customerCode, LocalDate promisedDate,
                              BigDecimal amount, BigDecimal balanceAtPromise, String channel, String note, String status, String createdBy,
                              Instant closedAt, Instant createdAt) {
    }
}
