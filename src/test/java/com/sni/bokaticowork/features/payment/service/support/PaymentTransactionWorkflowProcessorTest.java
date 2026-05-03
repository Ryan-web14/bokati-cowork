package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.contract.enums.ContractStatus;
import com.sni.bokaticowork.features.contract.model.Contract;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractService;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.SubscriptionStatusChangeRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.PassService;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentTransactionWorkflowProcessorTest {

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository billingDocumentRepository;

    @Mock
    private BillingDocumentService billingDocumentService;

    @Mock
    private TransactionContextResolver contextResolver;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private PassService passService;

    @Mock
    private BookingService bookingService;

    @Mock
    private ContractService contractService;

    @InjectMocks
    private PaymentTransactionWorkflowProcessor processor;

    @Test
    void shouldActivatePendingSubscriptionAfterSuccessfulFullyPaidInvoiceTransaction() {
        PaymentIntent intent = PaymentIntent.builder()
                .intentNumber("INT-001")
                .sourceType("BILLING_DOCUMENT")
                .sourceCode("INV-001")
                .status(PaymentIntentStatus.SUCCEEDED)
                .build();
        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionNumber("TXN-001")
                .paymentIntent(intent)
                .paymentMethod(PaymentMethod.CASH)
                .amount(new BigDecimal("100.0000"))
                .status(PaymentTransactionStatus.SUCCEEDED)
                .paidAt(Instant.parse("2026-04-27T10:00:00Z"))
                .build();
        Subscription subscription = Subscription.builder()
                .subscriptionNumber("SUB-001")
                .status(SubscriptionStatus.PENDING_ACTIVATION)
                .billingCycle(BillingCycle.MONTHLY)
                .totalAmount(new BigDecimal("100.0000"))
                .startDate(LocalDate.of(2026, 4, 27))
                .build();
        BillingDocument document = BillingDocument.builder()
                .documentNumber("INV-001")
                .documentType(BillingDocumentType.INVOICE)
                .status(BillingDocumentStatus.ISSUED)
                .balanceDue(BigDecimal.ZERO)
                .currency("XAF")
                .build();

        when(transactionRepository.findByTransactionNumber("TXN-001")).thenReturn(Optional.of(transaction));
        when(contextResolver.resolveSource("BILLING_DOCUMENT", "INV-001"))
                .thenReturn(new TransactionContextResolver.SourceView("SUBSCRIPTION", "SUB-001", "Abonnement SUB-001", true));
        when(subscriptionService.getForService("SUB-001")).thenReturn(subscription);
        when(billingDocumentRepository.findByDocumentNumber("INV-001")).thenReturn(Optional.of(document));
        when(contextResolver.resolveBillingDocumentSource(document))
                .thenReturn(new TransactionContextResolver.SourceView("SUBSCRIPTION", "SUB-001", "Abonnement SUB-001", true));

        processor.process(new PaymentTransactionWorkflowEvent("TXN-001", PaymentTransactionStatus.SUCCEEDED));

        verify(subscriptionService).activate(
                eq("SUB-001"),
                argThat((SubscriptionStatusChangeRequest request) ->
                        "SYSTEM".equals(request.changedBy())
                                && Boolean.FALSE.equals(request.cancelAtPeriodEnd())
                                && request.reason().contains("TXN-001"))
        );
    }

    @Test
    void shouldCancelSubscriptionAndTerminateContractAfterRefund() {
        PaymentIntent intent = PaymentIntent.builder()
                .intentNumber("INT-002")
                .sourceType("BILLING_DOCUMENT")
                .sourceCode("INV-002")
                .build();
        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionNumber("TXN-002")
                .paymentIntent(intent)
                .paymentMethod(PaymentMethod.CASH)
                .amount(new BigDecimal("100.0000"))
                .status(PaymentTransactionStatus.REFUNDED)
                .paidAt(Instant.parse("2026-04-27T11:00:00Z"))
                .build();
        Subscription subscription = Subscription.builder()
                .subscriptionNumber("SUB-002")
                .status(SubscriptionStatus.ACTIVE)
                .contractCode("CTR-002")
                .build();
        Contract contract = Contract.builder()
                .contractCode("CTR-002")
                .status(ContractStatus.ACTIVE)
                .build();

        when(transactionRepository.findByTransactionNumber("TXN-002")).thenReturn(Optional.of(transaction));
        when(contextResolver.resolveSource("BILLING_DOCUMENT", "INV-002"))
                .thenReturn(new TransactionContextResolver.SourceView("SUBSCRIPTION", "SUB-002", "Abonnement SUB-002", true));
        when(subscriptionService.getForService("SUB-002")).thenReturn(subscription);
        when(contractService.serviceByCode("CTR-002")).thenReturn(contract);

        processor.process(new PaymentTransactionWorkflowEvent("TXN-002", PaymentTransactionStatus.REFUNDED));

        verify(subscriptionService).cancel(
                eq("SUB-002"),
                argThat((SubscriptionStatusChangeRequest request) ->
                        "SYSTEM".equals(request.changedBy())
                                && request.reason().contains("TXN-002"))
        );
        verify(billingDocumentService).cancelAndArchive(eq("INV-002"), argThat(reason -> reason.contains("TXN-002")));
        verify(contractService).terminate(eq("CTR-002"), argThat(reason -> reason.contains("TXN-002")));
        verify(passService, never()).cancel(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldSystemCancelBookingAfterReverse() {
        PaymentIntent intent = PaymentIntent.builder()
                .intentNumber("INT-003")
                .sourceType("BILLING_DOCUMENT")
                .sourceCode("INV-003")
                .build();
        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionNumber("TXN-003")
                .paymentIntent(intent)
                .paymentMethod(PaymentMethod.CASH)
                .amount(new BigDecimal("50.0000"))
                .status(PaymentTransactionStatus.REVERSED)
                .paidAt(Instant.parse("2026-04-27T12:00:00Z"))
                .build();

        when(transactionRepository.findByTransactionNumber("TXN-003")).thenReturn(Optional.of(transaction));
        when(contextResolver.resolveSource("BILLING_DOCUMENT", "INV-003"))
                .thenReturn(new TransactionContextResolver.SourceView("BOOKING", "BKG-003", "Reservation BKG-003", true));

        processor.process(new PaymentTransactionWorkflowEvent("TXN-003", PaymentTransactionStatus.REVERSED));

        verify(billingDocumentService).cancelAndArchive(eq("INV-003"), argThat(reason -> reason.contains("TXN-003")));
        verify(bookingService).systemCancel(eq("BKG-003"), argThat(reason -> reason.contains("TXN-003")));
        verify(subscriptionService, never()).cancel(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }
}
