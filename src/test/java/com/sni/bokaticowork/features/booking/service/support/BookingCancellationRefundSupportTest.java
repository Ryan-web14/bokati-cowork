package com.sni.bokaticowork.features.booking.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentEditHistoryRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.model.CancellationPolicy;
import com.sni.bokaticowork.features.booking.repository.CancellationPolicyRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le sort de la facture quand la reservation est annulee.
 *
 * <p>Rien n'a ete encaisse : la facture est annulee et archivee. Elle restait vivante jusqu'ici,
 * ce qui revenait a reclamer le prix d'une reservation qui n'existe plus · silencieusement, pour
 * toute reservation annulee avant paiement.</p>
 *
 * <p>De l'argent a ete encaisse : la facture ne s'annule plus, elle se corrige par un avoir
 * chiffre par la politique d'annulation.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingCancellationRefundSupportTest {

    @Mock
    private BillingDocumentRepository billingDocumentRepository;
    @Mock
    private BillingDocumentEditHistoryRepository editHistoryRepository;
    @Mock
    private BillingDocumentService billingDocumentService;
    @Mock
    private CancellationPolicyRepository cancellationPolicyRepository;
    @Mock
    private PaymentAllocationRepository paymentAllocationRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BookingCancellationRefundSupport support;

    @Test
    void cancelsAndArchivesAnInvoiceThatWasNeverPaid() {
        givenInvoice(BigDecimal.ZERO);

        support.processCancellation(booking(), false);

        verify(billingDocumentService).cancelAndArchive(eq("INV-0001"),
                argThat(reason -> reason.contains("BKG-0001")));
        verify(billingDocumentService, never()).createCreditNote(anyString(), any());
    }

    @Test
    void cancelsTheInvoiceEvenWhenTheBookingNeverOccupiedItsSlot() {
        // Le cas le plus courant, et celui qui etait ignore : une reservation annulee avant
        // d'avoir ete confirmee laissait sa facture debout.
        givenInvoice(null);

        support.processCancellation(booking(), false);

        verify(billingDocumentService).cancelAndArchive(eq("INV-0001"), anyString());
    }

    @Test
    void issuesACreditNoteRatherThanCancellingWhenMoneyWasReceived() {
        givenInvoice(new BigDecimal("25000"));
        when(cancellationPolicyRepository.findAllByActiveTrueOrderByRuleOrderAsc())
                .thenReturn(List.of(policy(new BigDecimal("50"))));
        when(paymentAllocationRepository.findAllByBillingDocumentNumberFetchTransaction("INV-0001"))
                .thenReturn(List.of());

        support.processCancellation(booking(), true);

        verify(billingDocumentService, never()).cancelAndArchive(anyString(), anyString());
        verify(billingDocumentService).createCreditNote(eq("INV-0001"), any());
    }

    private void givenInvoice(BigDecimal paidAmount) {
        BillingDocument invoice = new BillingDocument();
        invoice.setDocumentNumber("INV-0001");
        invoice.setStatus(BillingDocumentStatus.ISSUED);
        invoice.setPaidAmount(paidAmount);
        when(billingDocumentRepository.findFirstBySourceAndType(
                "BILLABLE_ITEM", "BI-0001", BillingDocumentType.INVOICE.name()))
                .thenReturn(Optional.of(invoice));
    }

    private CancellationPolicy policy(BigDecimal refundPercentage) {
        return CancellationPolicy.builder()
                .hoursBeforeStart(0)
                .refundPercentage(refundPercentage)
                .ruleOrder(1)
                .active(true)
                .build();
    }

    private Booking booking() {
        return Booking.builder()
                .bookingNumber("BKG-0001")
                .billableNumber("BI-0001")
                .startedAt(LocalDateTime.now().plusDays(3))
                .build();
    }
}
