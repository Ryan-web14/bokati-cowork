package com.sni.bokaticowork.core.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import com.sni.bokaticowork.core.outbox.service.implementation.BillingDocumentCancellationOutboxEventProcessor;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le retour de la facture vers la reservation.
 *
 * <p>Jusqu'ici la propagation etait a sens unique : annuler une reservation reglait le sort de sa
 * facture, annuler la facture laissait la reservation confirmee et son creneau occupe, sans plus
 * rien pour la facturer.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingDocumentCancellationOutboxEventProcessorTest {

    @Mock
    private BillingDocumentRepository billingDocumentRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingService bookingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private BillingDocumentCancellationOutboxEventProcessor processor() {
        return new BillingDocumentCancellationOutboxEventProcessor(
                objectMapper, billingDocumentRepository, bookingRepository, bookingService);
    }

    @Test
    void handlesBothWaysOfCancellingAnInvoice() {
        // Une facture reglee ne se raye pas, elle se corrige par un avoir · la reservation
        // disparait dans les deux cas.
        assertTrue(processor().supports(event("BILLING_DOCUMENT_CANCELLED", "INV-0001")));
        assertTrue(processor().supports(event("BILLING_DOCUMENT_CANCELLED_VIA_CREDIT_NOTE", "INV-0001")));
        assertFalse(processor().supports(event("BILLING_DOCUMENT_ARCHIVED", "INV-0001")));
    }

    @Test
    void cancelsTheBookingBehindTheCancelledInvoice() {
        givenInvoiceOnBillableItem("INV-0001", "BI-0001");
        givenBooking("BI-0001", BookingStatus.CONFIRMED);

        processor().process(event("BILLING_DOCUMENT_CANCELLED", "INV-0001"));

        verify(bookingService).systemCancel(eq("BKG-0001"), argThat(reason -> reason.contains("INV-0001")));
    }

    @Test
    void leavesAnAlreadyCancelledBookingAlone() {
        // C'est le retour de la propagation inverse : la reservation annulee vient d'annuler sa
        // propre facture. Sans ce test, les deux sens se relanceraient l'un l'autre.
        givenInvoiceOnBillableItem("INV-0001", "BI-0001");
        givenBooking("BI-0001", BookingStatus.CANCELLED);

        processor().process(event("BILLING_DOCUMENT_CANCELLED", "INV-0001"));

        verify(bookingService, never()).systemCancel(anyString(), anyString());
    }

    @Test
    void ignoresAnInvoiceThatNeverFacturedABooking() {
        BillingDocument document = new BillingDocument();
        document.setDocumentNumber("INV-0002");
        document.setSourceType("SUBSCRIPTION");
        document.setSourceCode("SUB-0001");
        when(billingDocumentRepository.findByDocumentNumber("INV-0002")).thenReturn(Optional.of(document));

        processor().process(event("BILLING_DOCUMENT_CANCELLED", "INV-0002"));

        verify(bookingService, never()).systemCancel(anyString(), anyString());
    }

    private void givenInvoiceOnBillableItem(String documentNumber, String billableNumber) {
        BillingDocument document = new BillingDocument();
        document.setDocumentNumber(documentNumber);
        document.setSourceType("BILLABLE_ITEM");
        document.setSourceCode(billableNumber);
        when(billingDocumentRepository.findByDocumentNumber(documentNumber)).thenReturn(Optional.of(document));
    }

    private void givenBooking(String billableNumber, BookingStatus status) {
        when(bookingRepository.findByBillableNumber(billableNumber)).thenReturn(Optional.of(Booking.builder()
                .bookingNumber("BKG-0001")
                .billableNumber(billableNumber)
                .status(status)
                .build()));
    }

    private OutboxEvent event(String eventType, String documentNumber) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(eventType);
        event.setAggregateType("BILLING_DOCUMENT");
        event.setAggregateId(documentNumber);
        event.setPayload("{\"documentNumber\":\"" + documentNumber + "\"}");
        return event;
    }
}
