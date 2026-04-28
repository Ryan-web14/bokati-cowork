package com.sni.bokaticowork.features.billing.dto.response;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BillingDocumentResponse(
        String documentNumber,
        BillingDocumentType documentType,
        BillingDocumentStatus status,
        String customerType,
        String customerCode,
        String customerName,
        String customerEmail,
        String customerPhone,
        String billingAddressJson,
        Boolean customerRegistered,
        String sourceType,
        String sourceCode,
        String resolvedSourceType,
        String resolvedSourceCode,
        String resolvedSourceLabel,
        Boolean resolvedSourceRegistered,
        String title,
        String description,
        String terms,
        String currency,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal taxableAmount,
        BigDecimal vatAmount,
        BigDecimal additionalCentAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal balanceDue,
        LocalDate issueDate,
        LocalDate dueDate,
        Instant issuedAt,
        Instant sentAt,
        Instant paidAt,
        String metadataJson,
        List<BillingDocumentLineResponse> lines,
        List<BillingDocumentDiscountResponse> discounts,
        List<BillingDocumentTaxResponse> taxes,
        List<BillingDocumentClauseResponse> clauses
) {
}
