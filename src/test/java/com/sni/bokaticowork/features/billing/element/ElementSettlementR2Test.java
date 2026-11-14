package com.sni.bokaticowork.features.billing.element;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.element.dto.ElementDtos;
import com.sni.bokaticowork.features.billing.element.model.ElementRef;
import com.sni.bokaticowork.features.billing.element.service.BillingElementResolver;
import com.sni.bokaticowork.features.billing.element.service.ElementSettlementService;
import com.sni.bokaticowork.features.booking.dto.request.BookingStatusChangeRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingResponse;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot R2 : une reservation reglee par une facture rattachee est confirmee au solde, ou des le premier
 * paiement si l'administrateur l'a choisi ; une reservation deja confirmee est laissee telle quelle.
 */
class ElementSettlementR2Test {

    private final BillingElementResolver resolver = mock(BillingElementResolver.class);
    private final BookingService bookings = mock(BookingService.class);
    private final ElementSettlementService service;

    @SuppressWarnings("unchecked")
    ElementSettlementR2Test() {
        ObjectProvider<BookingService> bookingProvider = mock(ObjectProvider.class);
        when(bookingProvider.getObject()).thenReturn(bookings);
        service = new ElementSettlementService(resolver, mock(ObjectProvider.class), mock(ObjectProvider.class), mock(ObjectProvider.class),
                bookingProvider);
    }

    private void invoice(String balance, String mode) {
        when(resolver.document("FAC-1")).thenReturn(Optional.of(Map.<String, Object>of("total_amount", new BigDecimal("100000"),
                "balance_due", new BigDecimal(balance))));
        when(resolver.elements("FAC-1")).thenReturn(List.of(new ElementDtos.Settled(ElementRef.BOOKING, "BKG-1", "LINKED", mode)));
        when(resolver.invoices(new ElementRef(ElementRef.BOOKING, "BKG-1"))).thenReturn(List.of(new ElementDtos.Invoice("FAC-1", "INVOICE",
                new BigDecimal(balance).signum() == 0 ? "PAID" : "PARTIALLY_PAID", "CUSTOMER", "CUS-1", "XAF", new BigDecimal("100000"),
                new BigDecimal(balance), null, "LINKED", mode)));
    }

    private void booking(BookingStatus status) {
        BookingResponse response = mock(BookingResponse.class);
        when(response.status()).thenReturn(status);
        when(bookings.get("BKG-1")).thenReturn(response);
    }

    @Test
    void partialPaymentWaitsUnlessFirstPaymentWasChosen() {
        invoice("40000", ElementRef.FULL_SETTLEMENT);
        booking(BookingStatus.PENDING_PAYMENT);
        assertThat(service.settle("FAC-1", "test")).singleElement().extracting(ElementSettlementService.Outcome::result).isEqualTo("WAITING");
        verify(bookings, never()).confirm(any(), any());

        invoice("40000", ElementRef.FIRST_PAYMENT);
        assertThat(service.settle("FAC-1", "test")).singleElement().extracting(ElementSettlementService.Outcome::result).isEqualTo("APPLIED");
        verify(bookings).confirm(eq("BKG-1"), any(BookingStatusChangeRequest.class));
    }

    @Test
    void settledBookingIsConfirmedOnceAndAReplayChangesNothing() {
        invoice("0", ElementRef.FULL_SETTLEMENT);
        booking(BookingStatus.CONFIRMED);
        assertThat(service.settle("FAC-1", "rejeu")).singleElement().satisfies(outcome -> {
            assertThat(outcome.result()).isEqualTo("APPLIED");
            assertThat(outcome.detail()).isEqualTo("déjà CONFIRMED");
        });
        verify(bookings, never()).confirm(any(), any());
    }

    @Test
    void elementTypesAndModesAreChecked() {
        assertThat(ElementRef.of("booking", " BKG-1 ")).isEqualTo(new ElementRef(ElementRef.BOOKING, "BKG-1"));
        assertThatThrownBy(() -> ElementRef.of("FACTURE", "X")).isInstanceOf(BadRequestException.class).hasMessageContaining("Type d'élément inconnu");
        assertThat(ElementRef.mode(null)).isEqualTo(ElementRef.FULL_SETTLEMENT);
        assertThatThrownBy(() -> ElementRef.mode("PLUS_TARD")).isInstanceOf(BadRequestException.class);
        assertThat(ElementRef.typeOfBillableSource("SUBSCRIPTION_RECURRING")).isEqualTo(ElementRef.SUBSCRIPTION);
        assertThat(ElementRef.typeOfBillableSource("DEPOSIT")).isEqualTo(ElementRef.SUBSCRIPTION);
        assertThat(ElementRef.typeOfBillableSource("PASS_RENEWAL")).isEqualTo(ElementRef.PASS);
        assertThat(ElementRef.typeOfBillableSource("BOOKING")).isEqualTo(ElementRef.BOOKING);
        assertThat(ElementRef.typeOfBillableSource("INVENTORY_ITEM")).isNull();
    }
}
