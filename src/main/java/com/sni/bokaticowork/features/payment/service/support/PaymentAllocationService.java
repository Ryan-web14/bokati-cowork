package com.sni.bokaticowork.features.payment.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.service.support.BillingAutoInvoiceService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.payment.model.PaymentAllocation;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class PaymentAllocationService {

    private static final String BILLING_DOCUMENT_SOURCE = "BILLING_DOCUMENT";
    private static final String MULTI_BILLING_DOCUMENT_SOURCE = "MULTI_BILLING_DOCUMENT";
    private static final String RECOVERY_SOURCE = "PAYABLE_RECOVERY";

    private final PaymentAllocationRepository allocationRepository;
    private final BillingDocumentService billingDocumentService;
    private final BillingAutoInvoiceService autoInvoiceService;
    private final ObjectMapper objectMapper;

    public BigDecimal allocateIfBillingDocument(PaymentTransaction transaction) {
        PaymentIntent intent = transaction.getPaymentIntent();
        if (BILLING_DOCUMENT_SOURCE.equalsIgnoreCase(intent.getSourceType())) {
            return allocateOne(transaction, intent.getSourceCode(), transaction.getAmount());
        }
        if (MULTI_BILLING_DOCUMENT_SOURCE.equalsIgnoreCase(intent.getSourceType())) {
            BigDecimal remaining = transaction.getAmount();
            for (String documentNumber : Arrays.stream(intent.getSourceCode().split(",")).map(String::trim).filter(item -> !item.isBlank()).toList()) {
                remaining = allocateOne(transaction, documentNumber, remaining);
                if (remaining.signum() == 0) {
                    break;
                }
            }
            return remaining;
        }
        if (RECOVERY_SOURCE.equalsIgnoreCase(intent.getSourceType())) {
            BigDecimal remaining = transaction.getAmount();
            for (String documentNumber : recoveryDocumentNumbers(intent)) {
                remaining = allocateOne(transaction, documentNumber, remaining);
                if (remaining.signum() == 0) {
                    break;
                }
            }
            return remaining;
        }
        if (autoInvoiceService.shouldAutoInvoice(intent)) {
            BillingDocument invoice = autoInvoiceService.ensureInvoiceForPaymentTransaction(transaction);
            return allocateOne(transaction, invoice.getDocumentNumber(), transaction.getAmount());
        }
        return transaction.getAmount();
    }

    public void reverseAllocations(PaymentTransaction transaction, BigDecimal amount) {
        BigDecimal remaining = amount;
        for (PaymentAllocation allocation : allocationRepository.findAllByPaymentTransactionId(transaction.getId())) {
            if (remaining.signum() == 0) {
                return;
            }
            BigDecimal reversed = allocation.getAllocatedAmount().min(remaining);
            billingDocumentService.reversePayment(allocation.getBillingDocumentNumber(), reversed);
            remaining = remaining.subtract(reversed);
        }
    }

    private BigDecimal allocateOne(PaymentTransaction transaction, String documentNumber, BigDecimal amount) {
        BillingDocument before = billingDocumentService.serviceByNumber(documentNumber);
        BigDecimal allocated = amount.min(before.getBalanceDue());
        if (allocated.signum() == 0) {
            return amount;
        }
        BillingDocument document = billingDocumentService.applyPayment(documentNumber, allocated);
        allocationRepository.save(PaymentAllocation.builder()
                .paymentTransaction(transaction)
                .billingDocumentNumber(document.getDocumentNumber())
                .allocatedAmount(allocated)
                .build());
        return amount.subtract(allocated);
    }

    private java.util.List<String> recoveryDocumentNumbers(PaymentIntent intent) {
        if (intent == null || intent.getMetadataJson() == null || intent.getMetadataJson().isBlank()) {
            return java.util.List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(intent.getMetadataJson());
            JsonNode documentNumbers = root.get("documentNumbers");
            if (documentNumbers == null || !documentNumbers.isArray()) {
                return java.util.List.of();
            }
            java.util.List<String> values = new java.util.ArrayList<>();
            documentNumbers.forEach(node -> {
                if (node != null && node.isTextual() && !node.asText().isBlank()) {
                    values.add(node.asText().trim());
                }
            });
            return values;
        } catch (Exception ex) {
            return java.util.List.of();
        }
    }
}
