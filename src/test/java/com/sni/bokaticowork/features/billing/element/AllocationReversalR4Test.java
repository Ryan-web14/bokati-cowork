package com.sni.bokaticowork.features.billing.element;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.element.dto.ElementDtos;
import com.sni.bokaticowork.features.billing.element.model.ElementRef;
import com.sni.bokaticowork.features.billing.element.model.PaymentAllocationReversal;
import com.sni.bokaticowork.features.billing.element.repository.PaymentAllocationReversalRepository;
import com.sni.bokaticowork.features.billing.element.service.AllocationReversalService;
import com.sni.bokaticowork.features.billing.element.service.BillingElementResolver;
import com.sni.bokaticowork.features.billing.element.service.ElementSettlementService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.booking.dto.response.BookingResponse;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.payment.model.PaymentAllocation;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot R4 : l'affectation annulee remet la somme en attente, rouvre la facture et remet en attente
 * la reservation qu'elle avait confirmee ; une reservation deja commencee refuse le retour arriere.
 */
class AllocationReversalR4Test {

    private final PaymentAllocationRepository allocations = mock(PaymentAllocationRepository.class);
    private final PaymentAllocationReversalRepository reversals = mock(PaymentAllocationReversalRepository.class);
    private final BillingElementResolver resolver = mock(BillingElementResolver.class);
    private final ElementSettlementService settlements = mock(ElementSettlementService.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final BillingDocumentService documents = mock(BillingDocumentService.class);
    private final BookingService bookings = mock(BookingService.class);
    private final StaffProfileService staff = mock(StaffProfileService.class);
    private AllocationReversalService service;
    private PaymentAllocation allocation;

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(value);
        return provider;
    }

    @BeforeEach
    void setUp() {
        service = new AllocationReversalService(allocations, reversals, resolver, settlements, jdbc, provider(documents), provider(null),
                provider(null), provider(null), provider(null), provider(bookings), staff);
        when(staff.currentAuthor()).thenReturn(new StaffProfileService.Author("compta@elleaose.com", "Comptabilité", null));
        allocation = PaymentAllocation.builder().id(7L).paymentTransaction(PaymentTransaction.builder().transactionNumber("TXN-7").build())
                .billingDocumentNumber("FAC-7").allocatedAmount(new BigDecimal("50000")).allocationMode("LATE").build();
        when(allocations.findById(7L)).thenReturn(Optional.of(allocation));
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), anyString())).thenReturn(Boolean.TRUE);
        when(resolver.elements("FAC-7")).thenReturn(List.of(new ElementDtos.Settled(ElementRef.BOOKING, "BKG-7", "LINKED", ElementRef.FULL_SETTLEMENT)));
        when(settlements.fullySettled(any())).thenReturn(false);
    }

    private void booking(BookingStatus status) {
        BookingResponse response = mock(BookingResponse.class);
        when(response.status()).thenReturn(status);
        when(bookings.get("BKG-7")).thenReturn(response);
    }

    @Test
    void reopensTheInvoiceAndPutsTheBookingBackToPendingPayment() {
        booking(BookingStatus.CONFIRMED);
        AllocationReversalService.Result result = service.cancel(7L, new AllocationReversalService.CancelRequest(null, "Mauvaise facture"));

        verify(documents).reversePayment("FAC-7", new BigDecimal("50000"));
        verify(documents, never()).cancelAndArchive(anyString(), anyString());
        verify(bookings).revertConfirmation(eq("BKG-7"), anyString());
        verify(allocations).delete(allocation);
        ArgumentCaptor<PaymentAllocationReversal> saved = ArgumentCaptor.forClass(PaymentAllocationReversal.class);
        verify(reversals).save(saved.capture());
        assertThat(saved.getValue().getInvoiceOutcome()).isEqualTo("REOPEN");
        assertThat(saved.getValue().getAccountingRequired()).isTrue();
        assertThat(result.elementsReverted()).singleElement().asString().contains("BKG-7");
    }

    @Test
    void neutralizeCancelsTheInvoice() {
        booking(BookingStatus.PENDING_PAYMENT);
        service.cancel(7L, new AllocationReversalService.CancelRequest("neutralize", "Doublon"));
        verify(documents).cancelAndArchive(eq("FAC-7"), anyString());
        verify(bookings, never()).revertConfirmation(anyString(), anyString());
    }

    @Test
    void aStartedBookingRefusesTheRollbackAndNothingIsDeleted() {
        booking(BookingStatus.IN_PROGRESS);
        assertThatThrownBy(() -> service.cancel(7L, new AllocationReversalService.CancelRequest(null, "Erreur")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("déjà commencé");
        verify(allocations, never()).delete(any());
        verify(reversals, never()).save(any());
    }

    @Test
    void reasonAndOutcomeAreChecked() {
        assertThatThrownBy(() -> service.cancel(7L, new AllocationReversalService.CancelRequest(null, " ")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("motif");
        assertThatThrownBy(() -> service.cancel(7L, new AllocationReversalService.CancelRequest("SUPPRIMER", "x")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Sort de la facture inconnu");
    }
}
