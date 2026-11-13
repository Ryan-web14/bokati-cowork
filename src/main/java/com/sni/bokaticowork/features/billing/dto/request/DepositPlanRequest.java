package com.sni.bokaticowork.features.billing.dto.request;

import com.sni.bokaticowork.features.billing.enums.BillingDepositMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Plan d'acomptes (lot B3) : un ou plusieurs acomptes d'un coup, chacun en montant ou en
 * pourcentage (du total, ou des lignes choisies), avec son echeance.
 */
public record DepositPlanRequest(@NotEmpty List<@Valid Deposit> deposits) {

    /**
     * @param lineOrders  rangs des lignes servant de base au pourcentage ; vide : le total TTC
     * @param invoiceNow  vrai par defaut : la facture d'acompte est emise aussitot ; faux : acompte
     *                    prevu, facture plus tard ({@code PATCH /deposits/{id}/invoice})
     */
    public record Deposit(@Size(max = 255) String label, @NotNull BillingDepositMode mode, @NotNull @Positive BigDecimal value,
                          List<Integer> lineOrders, LocalDate dueDate, Boolean invoiceNow) {
    }
}
