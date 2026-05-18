package com.sni.bokaticowork.features.billing.dto.request;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateBillingDocumentRequest(
        @NotNull BillingDocumentType documentType,
        String customerType,
        String customerCode,
        String customerName,
        String customerEmail,
        String customerPhone,
        String billingAddressJson,
        String sourceType,
        String sourceCode,
        String title,
        String description,
        String terms,
        @NotBlank String currency,
        LocalDate issueDate,
        LocalDate dueDate,
        String metadataJson,
        @Valid @NotEmpty List<CreateBillingDocumentLineRequest> lines,
        @Valid List<CreateBillingDocumentDiscountRequest> discounts,
        @Valid List<CreateBillingDocumentClauseRequest> clauses,
        String paymentReference,
        String paymentInstructions,
        String bankDetailsJson,
        @Valid CreateBillingDocumentAdvanceRequest advance,
        String customerReference,
        String poNumber,
        String projectCode,
        String salespersonCode,
        String deliveryAddressJson,
        String language,
        java.math.BigDecimal exchangeRate,
        @Valid CreateEarlyPaymentDiscountRequest earlyPaymentDiscount
) {
}
