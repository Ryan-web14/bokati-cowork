package com.sni.bokaticowork.features.billing.service.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.dto.request.BillingAutoInvoiceRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Map;

import static java.math.BigDecimal.ONE;

@Component
@RequiredArgsConstructor
public class BillingAutoInvoiceService {

    private static final String BILLING_DOCUMENT_SOURCE = "BILLING_DOCUMENT";
    private static final String MULTI_BILLING_DOCUMENT_SOURCE = "MULTI_BILLING_DOCUMENT";
    private static final String PAYMENT_INTENT_SOURCE = "PAYMENT_INTENT";

    private final BillingDocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    @Lazy
    private final BillingDocumentService billingDocumentService;

    public BillingDocument ensureInvoiceForPaymentTransaction(PaymentTransaction transaction) {
        PaymentIntent intent = transaction.getPaymentIntent();
        if (BILLING_DOCUMENT_SOURCE.equalsIgnoreCase(intent.getSourceType())) {
            return billingDocumentService.serviceByNumber(intent.getSourceCode());
        }
        return ensureInvoice(new BillingAutoInvoiceRequest(
                intent.getCustomerType(),
                intent.getCustomerCode(),
                fallbackCustomerName(intent),
                null,
                null,
                null,
                PAYMENT_INTENT_SOURCE,
                intent.getIntentNumber(),
                invoiceTitle(intent),
                invoiceDescription(intent, transaction),
                transaction.getCurrency(),
                transaction.getAmount(),
                lineType(intent.getSourceType()),
                Boolean.FALSE,
                metadata(transaction)
        ));
    }

    public BillingDocument ensureInvoice(BillingAutoInvoiceRequest request) {
        validate(request);
        return documentRepository.findFirstBySourceAndType(
                        request.sourceType().trim(),
                        request.sourceCode().trim(),
                        BillingDocumentType.INVOICE.name()
                )
                .orElseGet(() -> createInvoice(request));
    }

    public boolean shouldAutoInvoice(PaymentIntent intent) {
        return intent != null
                && !BILLING_DOCUMENT_SOURCE.equalsIgnoreCase(intent.getSourceType())
                && !MULTI_BILLING_DOCUMENT_SOURCE.equalsIgnoreCase(intent.getSourceType());
    }

    private BillingDocument createInvoice(BillingAutoInvoiceRequest request) {
        BillingDocumentResponse created = billingDocumentService.create(
                new com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentRequest(
                        BillingDocumentType.INVOICE,
                        request.customerType(),
                        request.customerCode(),
                        request.customerName(),
                        request.customerEmail(),
                        request.customerPhone(),
                        request.billingAddressJson(),
                        request.sourceType().trim(),
                        request.sourceCode().trim(),
                        request.title(),
                        request.description(),
                        null,
                        request.currency(),
                        LocalDate.now(),
                        LocalDate.now(),
                        request.metadataJson(),
                        java.util.List.of(new com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentLineRequest(
                                1,
                                request.lineType() == null ? BillingLineType.SERVICE : request.lineType(),
                                null,
                                lineDescription(request),
                                null,
                                ONE,
                                request.amount(),
                                java.math.BigDecimal.ZERO,
                                java.math.BigDecimal.ZERO,
                                request.taxable() == null ? Boolean.FALSE : request.taxable(),
                                Boolean.TRUE.equals(request.taxable()),
                                java.math.BigDecimal.ZERO,
                                java.math.BigDecimal.ZERO,
                                request.sourceType().trim(),
                                request.sourceCode().trim(),
                                null, null, null, null
                        )),
                        java.util.List.of(),
                        defaultClauses(),
                        null, null, null, null,
                        null, null, null, null, null, null, null, null
                )
        );
        billingDocumentService.issue(created.documentNumber());
        return billingDocumentService.serviceByNumber(created.documentNumber());
    }

    private void validate(BillingAutoInvoiceRequest request) {
        if (!StringUtils.hasText(request.customerType()) || !StringUtils.hasText(request.customerCode())) {
            throw new BadRequestException("Customer type and code are required for automatic invoice generation");
        }
        if (!StringUtils.hasText(request.sourceType()) || !StringUtils.hasText(request.sourceCode())) {
            throw new BadRequestException("Source type and code are required for automatic invoice generation");
        }
        if (!StringUtils.hasText(request.currency())) {
            throw new BadRequestException("Currency is required for automatic invoice generation");
        }
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new BadRequestException("Amount must be positive for automatic invoice generation");
        }
    }

    private java.util.List<com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentClauseRequest> defaultClauses() {
        return java.util.List.of();
    }

    private String metadata(PaymentTransaction transaction) {
        PaymentIntent intent = transaction.getPaymentIntent();
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "generatedFrom", "PAYMENT_TRANSACTION",
                    "transactionNumber", transaction.getTransactionNumber(),
                    "intentNumber", intent.getIntentNumber(),
                    "paymentMethod", transaction.getPaymentMethod().name(),
                    "originalSourceType", nullSafe(intent.getSourceType()),
                    "originalSourceCode", nullSafe(intent.getSourceCode())
            ));
        } catch (JsonProcessingException ex) {
            return "{\"generatedFrom\":\"PAYMENT_TRANSACTION\"}";
        }
    }

    private BillingLineType lineType(String sourceType) {
        String normalized = sourceType == null ? "" : sourceType.toUpperCase();
        if (normalized.contains("BOOKING")) {
            return BillingLineType.BOOKING;
        }
        if (normalized.contains("PASS")) {
            return BillingLineType.PASS;
        }
        if (normalized.contains("ADDON")) {
            return BillingLineType.ADDON;
        }
        if (normalized.contains("SUBSCRIPTION")) {
            return BillingLineType.SUBSCRIPTION;
        }
        if (normalized.contains("OVERAGE")) {
            return BillingLineType.OVERAGE;
        }
        if (normalized.contains("INVENTORY") || normalized.contains("ITEM") || normalized.contains("PRODUCT")) {
            return BillingLineType.PRODUCT;
        }
        if (normalized.contains("WALLET")) {
            return BillingLineType.WALLET;
        }
        return BillingLineType.SERVICE;
    }

    private String invoiceTitle(PaymentIntent intent) {
        if (StringUtils.hasText(intent.getPurpose())) {
            return "Facture " + intent.getPurpose().trim();
        }
        return "Facture transaction " + intent.getIntentNumber();
    }

    private String invoiceDescription(PaymentIntent intent, PaymentTransaction transaction) {
        if (StringUtils.hasText(intent.getPurpose())) {
            return "Reglement pour " + intent.getPurpose().trim();
        }
        if (StringUtils.hasText(intent.getSourceType()) && StringUtils.hasText(intent.getSourceCode())) {
            return "Reglement lie a " + intent.getSourceType().trim() + " " + intent.getSourceCode().trim();
        }
        return "Reglement de la transaction " + transaction.getTransactionNumber();
    }

    private String lineDescription(PaymentIntent intent, PaymentTransaction transaction) {
        if (StringUtils.hasText(intent.getPurpose())) {
            return intent.getPurpose().trim();
        }
        if (StringUtils.hasText(intent.getSourceType())) {
            return "Transaction " + intent.getSourceType().trim();
        }
        return "Transaction " + transaction.getTransactionNumber();
    }

    private String lineDescription(BillingAutoInvoiceRequest request) {
        if (StringUtils.hasText(request.description())) {
            return request.description().trim();
        }
        if (StringUtils.hasText(request.title())) {
            return request.title().trim();
        }
        return "Transaction " + request.sourceCode().trim();
    }

    private String fallbackCustomerName(PaymentIntent intent) {
        return "Client " + intent.getCustomerCode();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
