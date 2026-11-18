package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.request.CreateCreditNoteRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.billing.service.support.BillableItemInvoiceSupport;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingLineRepository;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillableItemStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot R1, decision D1 : recalcul du prix d'une reservation modifiee, et son effet sur la facturation. */
class BookingRepricerTest {

    private final BookingPricingCalculator calculator = mock(BookingPricingCalculator.class);
    private final BookingLineRepository lines = mock(BookingLineRepository.class);
    private final BillableItemRepository items = mock(BillableItemRepository.class);
    private final BillingDocumentRepository documents = mock(BillingDocumentRepository.class);
    private final BillableItemInvoiceSupport invoices = mock(BillableItemInvoiceSupport.class);
    private final SequenceGeneratorFacade sequence = mock(SequenceGeneratorFacade.class);
    private final BookingEventWriter events = mock(BookingEventWriter.class);
    private final BillingDocumentService billing = mock(BillingDocumentService.class);
    private final BookingRepricer repricer = new BookingRepricer(calculator, lines, items, documents, invoices, sequence, events);

    private Booking booking;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(repricer, "billingDocumentService", billing);
        booking = Booking.builder().id(1L).bookingNumber("BKG-1").resource(Resource.builder().id(1L).code("RES-1").build())
                .startedAt(LocalDateTime.of(2026, 12, 7, 9, 0)).endedAt(LocalDateTime.of(2026, 12, 7, 12, 0))
                .totalAmount(new BigDecimal("10000")).currency("XAF").billableNumber("BI-1").build();
        when(lines.findByBookingId(1L)).thenReturn(List.of());
        when(sequence.next(anyString())).thenReturn("BI-2");
        when(items.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void newPrice(String amount) {
        when(calculator.calculate(any(), any(), any(), anyInt(), any(), any())).thenReturn(new BookingPricingCalculator.Price(
                ResourceBookingUnit.HOUR, new BigDecimal(amount).divide(BigDecimal.valueOf(3), 4, java.math.RoundingMode.HALF_UP), new BigDecimal("3"), new BigDecimal(amount), "XAF", null));
    }

    private void invoiced(BillableItemStatus status) {
        when(items.findByBillableNumber("BI-1")).thenReturn(Optional.of(BillableItem.builder().billableNumber("BI-1")
                .amount(new BigDecimal("10000")).status(status).build()));
    }

    @Test
    void anIncreaseOnAnInvoicedBookingIsInvoicedAsAComplement() {
        newPrice("15000");
        invoiced(BillableItemStatus.INVOICED);
        BillingDocumentResponse complement = mock(BillingDocumentResponse.class);
        when(complement.documentNumber()).thenReturn("INV-2");
        when(invoices.ensureInvoiced(any(), anyString(), anyString())).thenReturn(complement);

        BookingRepricer.Outcome outcome = repricer.reprice(booking, "nouvel horaire");

        ArgumentCaptor<BillableItem> item = ArgumentCaptor.forClass(BillableItem.class);
        verify(invoices).ensureInvoiced(item.capture(), anyString(), anyString());
        assertThat(item.getValue().getAmount()).isEqualByComparingTo("5000");
        assertThat(outcome.adjustmentDocument()).isEqualTo("INV-2");
        assertThat(booking.getTotalAmount()).isEqualByComparingTo("15000");
        verify(events).write(eq(booking), eq(BookingEventType.BOOKING_PRICE_ADJUSTED), anyString(), anyString(), any());
    }

    @Test
    void aDecreaseOnAnInvoicedBookingIsCredited() {
        newPrice("7000");
        invoiced(BillableItemStatus.INVOICED);
        BillingDocument invoice = BillingDocument.builder().documentNumber("INV-1").build();
        invoice.setBalanceDue(new BigDecimal("10000"));
        when(documents.findFirstBySourceAndType("BILLABLE_ITEM", "BI-1", "INVOICE")).thenReturn(Optional.of(invoice));
        BillingDocumentResponse credit = mock(BillingDocumentResponse.class);
        when(credit.documentNumber()).thenReturn("AV-1");
        when(billing.createCreditNote(eq("INV-1"), any())).thenReturn(credit);

        BookingRepricer.Outcome outcome = repricer.reprice(booking, "ressource RES-1 → RES-2");

        ArgumentCaptor<CreateCreditNoteRequest> request = ArgumentCaptor.forClass(CreateCreditNoteRequest.class);
        verify(billing).createCreditNote(eq("INV-1"), request.capture());
        assertThat(request.getValue().amount()).isEqualByComparingTo("3000");
        assertThat(request.getValue().applyImmediately()).isTrue();
        assertThat(outcome.adjustmentDocument()).isEqualTo("AV-1");
    }

    @Test
    void nothingInvoicedYetJustFollowsTheNewPrice() {
        newPrice("12000");
        BillableItem pending = BillableItem.builder().billableNumber("BI-1").amount(new BigDecimal("10000")).status(BillableItemStatus.PENDING).build();
        when(items.findByBillableNumber("BI-1")).thenReturn(Optional.of(pending));

        repricer.reprice(booking, "nouvel horaire");

        assertThat(pending.getAmount()).isEqualByComparingTo("12000");
        verify(invoices, never()).ensureInvoiced(any(), anyString(), anyString());
        verify(billing, never()).createCreditNote(anyString(), any());
    }

    @Test
    void anUnchangedPriceLeavesBillingAlone() {
        newPrice("10000");
        invoiced(BillableItemStatus.INVOICED);

        BookingRepricer.Outcome outcome = repricer.reprice(booking, "nouvel horaire");

        assertThat(outcome.difference()).isZero();
        verify(events, never()).write(any(), any(), anyString(), anyString(), any());
    }
}
