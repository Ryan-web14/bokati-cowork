package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

public record CreateManualBillingDocumentRequest(
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
        @Valid CreateEarlyPaymentDiscountRequest earlyPaymentDiscount,
        /**
         * Comme a la creation d'un document : vide, la facture d'un membre suit le mode de son
         * rattachement ; {@code NONE}, le membre seul ; un code client, cette organisation, a
         * laquelle le membre doit etre rattache. {@code customerCompanyCode} est accepte aussi.
         */
        @com.fasterxml.jackson.annotation.JsonAlias("customerCompanyCode") String companyCustomerCode
) {
    @com.fasterxml.jackson.annotation.JsonCreator
    public CreateManualBillingDocumentRequest {
    }

    /** Sans organisation precisee · la facture suit le mode du rattachement du membre. */
    public CreateManualBillingDocumentRequest(String customerType, String customerCode, String customerName,
                                              String customerEmail, String customerPhone, String billingAddressJson,
                                              String sourceType, String sourceCode, String title, String description,
                                              String terms, String currency, LocalDate issueDate, LocalDate dueDate,
                                              String metadataJson, List<CreateBillingDocumentLineRequest> lines,
                                              List<CreateBillingDocumentDiscountRequest> discounts,
                                              List<CreateBillingDocumentClauseRequest> clauses, String paymentReference,
                                              String paymentInstructions, String bankDetailsJson,
                                              CreateBillingDocumentAdvanceRequest advance, String customerReference,
                                              String poNumber, String projectCode, String salespersonCode,
                                              String deliveryAddressJson, String language, java.math.BigDecimal exchangeRate,
                                              CreateEarlyPaymentDiscountRequest earlyPaymentDiscount) {
        this(customerType, customerCode, customerName, customerEmail, customerPhone, billingAddressJson, sourceType,
                sourceCode, title, description, terms, currency, issueDate, dueDate, metadataJson, lines, discounts,
                clauses, paymentReference, paymentInstructions, bankDetailsJson, advance, customerReference, poNumber,
                projectCode, salespersonCode, deliveryAddressJson, language, exchangeRate, earlyPaymentDiscount, null);
    }
}
