package com.sni.bokaticowork.features.booking.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.billing.dto.request.CreateCreditNoteRequest;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentEditHistoryRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.model.CancellationPolicy;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.repository.CancellationPolicyRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Une reservation annulee dans la facture commune d'un lot multi-ressources : seule sa part est
 * retiree, la facture et les autres reservations du membre restent.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingBatchCancellationTest {

    @Mock private BillingDocumentRepository billingDocumentRepository;
    @Mock private BillingDocumentEditHistoryRepository editHistoryRepository;
    @Mock private BillingDocumentService billingDocumentService;
    @Mock private CancellationPolicyRepository cancellationPolicyRepository;
    @Mock private PaymentAllocationRepository paymentAllocationRepository;
    @Mock private PaymentService paymentService;
    @Mock private ObjectMapper objectMapper;
    @Mock private BillableItemRepository billableItemRepository;
    @Mock private BookingRepository bookingRepository;

    @InjectMocks private BookingCancellationRefundSupport support;

    private BillingDocument invoice;

    @BeforeEach
    void setUp() {
        invoice = new BillingDocument();
        invoice.setId(77L);
        invoice.setDocumentNumber("INV-LOT");
        invoice.setDocumentType(BillingDocumentType.INVOICE);
        invoice.setStatus(BillingDocumentStatus.ISSUED);
        invoice.setSourceType("BILLABLE_ITEM");
        invoice.setSourceCode("BI-1,BI-2");
        // La recherche exacte ne trouve pas une facture qui regle plusieurs elements
        when(billingDocumentRepository.findFirstBySourceAndType(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(billableItemRepository.findByBillableNumber("BI-1"))
                .thenReturn(Optional.of(BillableItem.builder().billableNumber("BI-1").invoiceId(77L).build()));
        when(billingDocumentRepository.findById(77L)).thenReturn(Optional.of(invoice));
    }

    private static Booking booking(String number, String billable, BookingStatus status) {
        return Booking.builder()
                .bookingNumber(number)
                .billableNumber(billable)
                .batchNumber("BKL-1")
                .paymentMode(BookingPaymentMode.DIRECT)
                .totalAmount(new BigDecimal("20000"))
                .status(status)
                .startedAt(LocalDateTime.now().plusDays(3))
                .build();
    }

    @Test
    @DisplayName("Rien d'encaisse, une autre reservation reste : un avoir du montant de la reservation, pas d'annulation de la facture")
    void unpaidWithOthersLeftGetsACreditNote() {
        when(bookingRepository.findByBillableNumber("BI-2")).thenReturn(Optional.of(booking("BKG-2", "BI-2", BookingStatus.PENDING_PAYMENT)));

        support.processCancellation(booking("BKG-1", "BI-1", BookingStatus.CANCELLED), false);

        ArgumentCaptor<CreateCreditNoteRequest> credit = ArgumentCaptor.forClass(CreateCreditNoteRequest.class);
        verify(billingDocumentService).createCreditNote(eq("INV-LOT"), credit.capture());
        assertThat(credit.getValue().amount()).isEqualByComparingTo("20000");
        assertThat(credit.getValue().reason()).contains("BKG-1").contains("BKL-1");
        verify(billingDocumentService, never()).cancelAndArchive(anyString(), anyString());
    }

    @Test
    @DisplayName("Rien d'encaisse, plus aucune reservation : la facture est annulee")
    void unpaidLastBookingCancelsTheInvoice() {
        when(bookingRepository.findByBillableNumber("BI-2")).thenReturn(Optional.of(booking("BKG-2", "BI-2", BookingStatus.CANCELLED)));

        support.processCancellation(booking("BKG-1", "BI-1", BookingStatus.CANCELLED), false);

        verify(billingDocumentService).cancelAndArchive(eq("INV-LOT"), anyString());
        verify(billingDocumentService, never()).createCreditNote(anyString(), any());
    }

    @Test
    @DisplayName("Deja encaisse : la politique d'annulation s'applique a la part de la reservation seulement")
    void paidAppliesThePolicyToTheBookingShare() {
        invoice.setPaidAmount(new BigDecimal("40000"));
        when(cancellationPolicyRepository.findAllByActiveTrueOrderByRuleOrderAsc()).thenReturn(List.of(
                CancellationPolicy.builder().hoursBeforeStart(0).refundPercentage(new BigDecimal("50")).ruleOrder(1).active(true).build()));
        when(paymentAllocationRepository.findAllByBillingDocumentNumberFetchTransaction("INV-LOT")).thenReturn(List.of());

        support.processCancellation(booking("BKG-1", "BI-1", BookingStatus.CANCELLED), true);

        ArgumentCaptor<CreateCreditNoteRequest> credit = ArgumentCaptor.forClass(CreateCreditNoteRequest.class);
        verify(billingDocumentService).createCreditNote(eq("INV-LOT"), credit.capture());
        assertThat(credit.getValue().amount()).isEqualByComparingTo("10000");
        verify(billingDocumentService, never()).cancelAndArchive(anyString(), anyString());
    }
}
