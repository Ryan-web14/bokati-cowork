package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Sélection des lignes optionnelles à inclure dans le devis.
 * Les lignes optionnelles non listées ici sont supprimées.
 * Les lignes non-optionnelles ne sont pas affectées.
 */
public record SelectQuoteOptionsRequest(
        @NotNull List<Integer> selectedLineOrders
) {
}
