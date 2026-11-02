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
        @Valid CreateEarlyPaymentDiscountRequest earlyPaymentDiscount,
        /**
         * L'organisation pour le compte de laquelle un membre est facture · vide : selon le mode de
         * son rattachement (automatique ou pour le membre seul) ; {@code NONE} : le membre seul,
         * meme en mode automatique ; un code client : cette organisation, a laquelle le membre doit
         * etre rattache.
         */
        String companyCustomerCode
) {
    /** Le membre seul, explicitement · ignore le mode automatique de son rattachement. */
    public static final String NO_COMPANY = "NONE";

    @com.fasterxml.jackson.annotation.JsonCreator
    public CreateBillingDocumentRequest {
    }

    /** Sans organisation precisee · la facture suit le mode du rattachement du membre. */
    public CreateBillingDocumentRequest(BillingDocumentType documentType, String customerType, String customerCode,
                                        String customerName, String customerEmail, String customerPhone,
                                        String billingAddressJson, String sourceType, String sourceCode, String title,
                                        String description, String terms, String currency, LocalDate issueDate,
                                        LocalDate dueDate, String metadataJson,
                                        List<CreateBillingDocumentLineRequest> lines,
                                        List<CreateBillingDocumentDiscountRequest> discounts,
                                        List<CreateBillingDocumentClauseRequest> clauses, String paymentReference,
                                        String paymentInstructions, String bankDetailsJson,
                                        CreateBillingDocumentAdvanceRequest advance, String customerReference,
                                        String poNumber, String projectCode, String salespersonCode,
                                        String deliveryAddressJson, String language, java.math.BigDecimal exchangeRate,
                                        CreateEarlyPaymentDiscountRequest earlyPaymentDiscount) {
        this(documentType, customerType, customerCode, customerName, customerEmail, customerPhone, billingAddressJson,
                sourceType, sourceCode, title, description, terms, currency, issueDate, dueDate, metadataJson, lines,
                discounts, clauses, paymentReference, paymentInstructions, bankDetailsJson, advance, customerReference,
                poNumber, projectCode, salespersonCode, deliveryAddressJson, language, exchangeRate, earlyPaymentDiscount,
                null);
    }
}
