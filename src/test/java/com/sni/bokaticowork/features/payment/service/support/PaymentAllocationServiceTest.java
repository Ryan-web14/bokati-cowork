package com.sni.bokaticowork.features.payment.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.billing.service.support.BillingAutoInvoiceService;
import com.sni.bokaticowork.features.payment.model.PaymentAllocation;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentAllocationServiceTest {

    private final PaymentAllocationRepository allocationRepository = mock(PaymentAllocationRepository.class);
    private final BillingDocumentService billingDocumentService = mock(BillingDocumentService.class);
    private final BillingAutoInvoiceService autoInvoiceService = mock(BillingAutoInvoiceService.class);

    private final PaymentAllocationService service = new PaymentAllocationService(
            allocationRepository, billingDocumentService, autoInvoiceService, new ObjectMapper());

    @Test
    void shouldAllocateOnceToTheTargetInvoice() {
        PaymentTransaction transaction = transaction(new BigDecimal("500"), "INV-001");
        when(allocationRepository.existsByPaymentTransaction_IdAndBillingDocumentNumber(1L, "INV-001"))
                .thenReturn(false);
        when(billingDocumentService.serviceByNumber("INV-001")).thenReturn(invoice("500"));
        when(billingDocumentService.applyPayment("INV-001", new BigDecimal("500"))).thenReturn(invoice("0"));

        BigDecimal overpayment = service.allocateIfBillingDocument(transaction);

        assertThat(overpayment).isEqualByComparingTo("0");
        verify(allocationRepository).save(any(PaymentAllocation.class));
    }

    @Test
    void shouldNotReallocateATransactionAlreadyImputedToTheSameInvoice() {
        PaymentTransaction transaction = transaction(new BigDecimal("500"), "INV-001");
        when(allocationRepository.existsByPaymentTransaction_IdAndBillingDocumentNumber(1L, "INV-001"))
                .thenReturn(true);

        BigDecimal overpayment = service.allocateIfBillingDocument(transaction);

        // Le rattrapage PAYMENT_ALLOCATION_RETRY rejoue l'imputation : sans cette garde,
        // le montant paye de la facture serait double a chaque tentative.
        assertThat(overpayment).isEqualByComparingTo("500");
        verify(billingDocumentService, never()).applyPayment(anyString(), any());
        verify(allocationRepository, never()).save(any(PaymentAllocation.class));
    }

    private PaymentTransaction transaction(BigDecimal amount, String documentNumber) {
        PaymentIntent intent = PaymentIntent.builder()
                .intentNumber("PI-001")
                .sourceType("BILLING_DOCUMENT")
                .sourceCode(documentNumber)
                .build();
        return PaymentTransaction.builder()
                .id(1L)
                .transactionNumber("TXN-001")
                .paymentIntent(intent)
                .amount(amount)
                .build();
    }

    private BillingDocument invoice(String balanceDue) {
        return BillingDocument.builder()
                .documentNumber("INV-001")
                .balanceDue(new BigDecimal(balanceDue))
                .build();
    }
}
