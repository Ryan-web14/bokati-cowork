package com.sni.bokaticowork.features.billing.dto.request;

import com.sni.bokaticowork.features.billing.enums.BillingAdvanceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Demande d'acompte sur un devis ou une facture.
 *
 * <p>La base de calcul est déterminée ainsi :
 * <ul>
 *   <li>Si {@code includedLineOrders} est renseigné : uniquement ces lignes.</li>
 *   <li>Si {@code excludedLineOrders} est renseigné : toutes les lignes sauf celles-ci.</li>
 *   <li>Sinon : toutes les lignes du document.</li>
 * </ul>
 */
public record CreateBillingDocumentAdvanceRequest(
        @NotNull BillingAdvanceType advanceType,
        @NotNull @Positive BigDecimal advanceValue,
        List<Integer> includedLineOrders,
        List<Integer> excludedLineOrders,
        String paymentReference,
        String referenceLabel,
        LocalDate dueDate,
        String notes
) {
}