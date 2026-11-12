package com.sni.bokaticowork.features.subscription.subscription.service.support.subscription;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.PaymentAllocation;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Rattachement d'une facture ou d'un paiement deja faits a un abonnement, et sa validation. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionBillingLinkServiceTest {

    @Mock private BillingDocumentRepository documentRepository;
    @Mock private BillingDocumentService documentService;
    @Mock private PaymentTransactionRepository transactionRepository;
    @Mock private PaymentAllocationRepository allocationRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private BillableItemRepository billableItemRepository;
    @Mock private SubscriptionEventWriter eventWriter;
    @Mock private SubscriptionLifecycleOperator lifecycleOperator;
    @InjectMocks private SubscriptionBillingLinkService service;

    private BillingDocument invoice;

    @BeforeEach
    void setUp() {
        invoice = BillingDocument.builder().id(7L).documentNumber("FAC-1").documentType(BillingDocumentType.INVOICE).status(BillingDocumentStatus.PAID)
                .companyCustomerCode("CUS-COM-1").currency("XAF").totalAmount(new BigDecimal("150000")).balanceDue(BigDecimal.ZERO).build();
        when(documentRepository.findByDocumentNumber("FAC-1")).thenReturn(Optional.of(invoice));
        when(subscriptionRepository.findNumbersLinkedToInvoice(anyString())).thenReturn(List.of());
    }

    @Test
    @DisplayName("Une facture soldee au nom du souscripteur se rattache et permet la validation")
    void paidInvoice() {
        SubscriptionBillingLinkService.Link link = service.resolve(List.of("CUS-COM-1"), "FAC-1", null, null, new BigDecimal("150000"));
        assertThat(link.paid()).isTrue();
        assertThat(link.warnings()).isEmpty();
    }

    @Test
    @DisplayName("Une facture d'un autre client, annulee, ou deja rattachee est refusee ; une facture non soldee ou plus faible avertit")
    void refusalsAndWarnings() {
        assertThatThrownBy(() -> service.resolve(List.of("CUS-9"), "FAC-1", null, null, null)).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("autre client");
        when(subscriptionRepository.findNumbersLinkedToInvoice("FAC-1")).thenReturn(List.of("SUB-OTHER"));
        assertThatThrownBy(() -> service.resolve(List.of("CUS-COM-1"), "FAC-1", null, "SUB-NEW", null)).isInstanceOf(ConflictException.class);
        when(subscriptionRepository.findNumbersLinkedToInvoice("FAC-1")).thenReturn(List.of());
        invoice.setBalanceDue(new BigDecimal("50000"));
        SubscriptionBillingLinkService.Link link = service.resolve(List.of("CUS-COM-1"), "FAC-1", null, null, new BigDecimal("200000"));
        assertThat(link.paid()).isFalse();
        assertThat(link.warnings()).hasSize(2);
        invoice.setStatus(BillingDocumentStatus.CANCELLED);
        assertThatThrownBy(() -> service.resolve(List.of("CUS-COM-1"), "FAC-1", null, null, null)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Un paiement seul designe la facture qu'il regle ; il doit etre reussi")
    void paymentDesignatesTheInvoice() {
        PaymentTransaction transaction = PaymentTransaction.builder().id(3L).transactionNumber("TXN-1").status(PaymentTransactionStatus.SUCCEEDED)
                .paymentIntent(PaymentIntent.builder().customerCode("CUS-COM-1").build()).build();
        when(transactionRepository.findByTransactionNumber("TXN-1")).thenReturn(Optional.of(transaction));
        when(allocationRepository.findAllByPaymentTransactionId(3L)).thenReturn(List.of(PaymentAllocation.builder().billingDocumentNumber("FAC-1").build()));
        SubscriptionBillingLinkService.Link link = service.resolve(List.of("CUS-COM-1"), null, "TXN-1", null, null);
        assertThat(link.invoiceNumber()).isEqualTo("FAC-1");
        assertThat(link.paymentNumber()).isEqualTo("TXN-1");
        transaction.setStatus(PaymentTransactionStatus.FAILED);
        assertThatThrownBy(() -> service.resolve(List.of("CUS-COM-1"), null, "TXN-1", null, null)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Sur un abonnement en attente : sa facture initiale impayee est annulee, il est valide sur la facture existante soldee")
    void linkAndValidate() {
        Subscription subscription = Subscription.builder().id(1L).subscriptionNumber("SUB-1").subscriberCode("CUS-COM-1")
                .status(SubscriptionStatus.PENDING_ACTIVATION).totalAmount(new BigDecimal("150000")).build();
        when(billableItemRepository.findAllBySourceId("SUB-1")).thenReturn(List.of(BillableItem.builder().invoiceId(9L).build()));
        BillingDocument own = BillingDocument.builder().id(9L).documentNumber("FAC-OWN").documentType(BillingDocumentType.INVOICE)
                .status(BillingDocumentStatus.ISSUED).paidAmount(BigDecimal.ZERO).locked(false).build();
        when(documentRepository.findById(9L)).thenReturn(Optional.of(own));

        SubscriptionBillingLinkService.LinkResult result = service.linkAndValidate(subscription, List.of("CUS-COM-1"), "FAC-1", null, true, false, false, "admin");

        verify(documentService).cancelAndArchive(eq("FAC-OWN"), contains("FAC-1"));
        assertThat(result.cancelledInvoices()).containsExactly("FAC-OWN");
        verify(lifecycleOperator).activate(eq(subscription), contains("FAC-1"), eq("admin"));
        assertThat(subscription.getMetadataJson()).contains("\"linkedInvoiceNumber\":\"FAC-1\"");
    }

    @Test
    @DisplayName("Facture non soldee : l'abonnement attend, sauf validation forcee par un administrateur")
    void unpaidInvoiceWaitsUnlessForced() {
        invoice.setBalanceDue(new BigDecimal("10000"));
        Subscription subscription = Subscription.builder().id(1L).subscriptionNumber("SUB-1").subscriberCode("CUS-COM-1")
                .status(SubscriptionStatus.PENDING_ACTIVATION).build();
        when(billableItemRepository.findAllBySourceId("SUB-1")).thenReturn(List.of());

        SubscriptionBillingLinkService.LinkResult waiting = service.linkAndValidate(subscription, List.of("CUS-COM-1"), "FAC-1", null, true, true, false, "staff");
        verify(lifecycleOperator, never()).activate(any(), anyString(), anyString());
        assertThat(waiting.warnings()).anyMatch(w -> w.contains("laissé en attente"));

        service.linkAndValidate(subscription, List.of("CUS-COM-1"), "FAC-1", null, true, true, true, "admin");
        verify(lifecycleOperator).activate(eq(subscription), contains("forcée"), eq("admin"));
    }
}
