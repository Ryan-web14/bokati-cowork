package com.sni.bokaticowork.features.billing.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Édition d'un document après création.
 * DRAFT         → tous les champs modifiables.
 * ISSUED / SENT → uniquement dueDate, terms, paymentInstructions, internalNotes.
 */
public record UpdateBillingDocumentRequest(
        String title,
        String description,
        String terms,
        LocalDate issueDate,
        LocalDate dueDate,
        String customerReference,
        String poNumber,
        String projectCode,
        String salespersonCode,
        String deliveryAddressJson,
        String language,
        BigDecimal exchangeRate,
        String paymentReference,
        String paymentInstructions,
        String bankDetailsJson,
        String internalNotes,
        List<UpdateBillingDocumentLineRequest> lines,
        List<CreateBillingDocumentDiscountRequest> discounts,
        List<CreateBillingDocumentClauseRequest> clauses
) {
}
