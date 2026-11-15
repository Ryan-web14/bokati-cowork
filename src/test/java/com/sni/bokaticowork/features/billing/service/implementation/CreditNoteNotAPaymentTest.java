package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentAdvanceRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingLifecycleSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Un avoir impute sur sa facture n'est pas un paiement. Cas de l'annulation d'une reservation payee
 * 10 000, remboursee a 50 % : avoir de 5 000, remboursement de 5 000 ; la facture est remboursee, pas
 * « partiellement payee ». Facture non payee : un avoir partiel reduit le reste du sans la dire payee.
 */
class CreditNoteNotAPaymentTest {

    private final BillingDocumentRepository documents = mock(BillingDocumentRepository.class);
    private BillingDocumentServiceImpl service;
    private BillingDocument invoice;

    @BeforeEach
    void setUp() {
        service = mock(BillingDocumentServiceImpl.class, Mockito.CALLS_REAL_METHODS);
        BillingDocumentAdvanceRepository advances = mock(BillingDocumentAdvanceRepository.class);
        when(advances.findByDocument(any())).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(service, "documentRepository", documents);
        ReflectionTestUtils.setField(service, "advanceRepository", advances);
        ReflectionTestUtils.setField(service, "lifecycleSupport", mock(BillingLifecycleSupport.class));
        ReflectionTestUtils.setField(service, "entityManager", mock(EntityManager.class));
        invoice = BillingDocument.builder().documentNumber("FAC-1").documentType(BillingDocumentType.INVOICE).status(BillingDocumentStatus.ISSUED)
                .totalAmount(new BigDecimal("10000")).paidAmount(BigDecimal.ZERO).balanceDue(new BigDecimal("10000")).build();
        when(documents.findByDocumentNumber("FAC-1")).thenReturn(Optional.of(invoice));
        when(documents.lockByDocumentNumber("FAC-1")).thenReturn(Optional.of(invoice));
        when(documents.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void cancelledBookingRefundedAtHalfLeavesTheInvoiceRefunded() {
        service.applyPayment("FAC-1", new BigDecimal("10000"));
        assertThat(invoice.getStatus()).isEqualTo(BillingDocumentStatus.PAID);

        service.applyCredit("FAC-1", new BigDecimal("5000"));
        assertThat(invoice.getStatus()).isEqualTo(BillingDocumentStatus.PAID);
        assertThat(invoice.getCreditedAmount()).isEqualByComparingTo("5000");
        assertThat(invoice.getPaidAmount()).isEqualByComparingTo("10000");

        service.reversePayment("FAC-1", new BigDecimal("5000"));
        assertThat(invoice.getStatus()).isEqualTo(BillingDocumentStatus.REFUNDED);
        assertThat(invoice.getPaidAmount()).isEqualByComparingTo("5000");
        assertThat(invoice.getBalanceDue()).isEqualByComparingTo("0");
    }

    @Test
    void partialCreditOnAnUnpaidInvoiceReducesTheBalanceWithoutCallingItPaid() {
        service.applyCredit("FAC-1", new BigDecimal("2500"));
        assertThat(invoice.getStatus()).isEqualTo(BillingDocumentStatus.ISSUED);
        assertThat(invoice.getPaidAmount()).isEqualByComparingTo("0");
        assertThat(invoice.getBalanceDue()).isEqualByComparingTo("7500");

        service.applyPayment("FAC-1", new BigDecimal("9000"));
        assertThat(invoice.getPaidAmount()).isEqualByComparingTo("7500");
        assertThat(invoice.getStatus()).isEqualTo(BillingDocumentStatus.PAID);
    }

    @Test
    void fullCreditOnAnUnpaidInvoiceCancelsIt() {
        service.applyCredit("FAC-1", new BigDecimal("10000"));
        assertThat(invoice.getStatus()).isEqualTo(BillingDocumentStatus.CANCELLED);
        assertThat(invoice.getBalanceDue()).isEqualByComparingTo("0");
    }
}
