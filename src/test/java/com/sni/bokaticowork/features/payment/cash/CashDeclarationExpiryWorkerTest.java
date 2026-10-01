package com.sni.bokaticowork.features.payment.cash;

import com.sni.bokaticowork.features.booking.dto.response.BookingResponse;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.payment.cash.enums.CashDeclarationStatus;
import com.sni.bokaticowork.features.payment.cash.model.CashPaymentDeclaration;
import com.sni.bokaticowork.features.payment.cash.repository.CashPaymentDeclarationRepository;
import com.sni.bokaticowork.features.payment.cash.service.CashDeclarationNotifier;
import com.sni.bokaticowork.features.payment.cash.service.CashPaymentDeclarationService;
import com.sni.bokaticowork.features.payment.cash.worker.CashDeclarationExpiryWorker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Une annonce que personne n'honore tombe · et rend le creneau qu'elle tenait.
 *
 * <p>Ce qui tombe et ce qui ne tombe pas : la reservation est annulee, la facture reste due. Le
 * client n'a pas paye, et une annonce non honoree n'a jamais eteint une creance.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CashDeclarationExpiryWorkerTest {

    @Mock private CashPaymentDeclarationRepository repository;
    @Mock private CashPaymentDeclarationService declarationService;
    @Mock private CashDeclarationNotifier notifier;
    @Mock private BookingService bookingService;
    @InjectMocks private CashDeclarationExpiryWorker worker;

    private CashPaymentDeclaration overdue(String bookingNumber) {
        return CashPaymentDeclaration.builder()
                .declarationNumber("ESP-2026-000001")
                .documentNumber("INV-1")
                .bookingNumber(bookingNumber)
                .customerType("MEMBER")
                .customerCode("MBR-1")
                .amount(new BigDecimal("25000"))
                .currency("XAF")
                .status(CashDeclarationStatus.AWAITING_CONFIRMATION)
                .declaredAt(Instant.now().minusSeconds(90_000))
                .expiresAt(Instant.now().minusSeconds(3_600))
                .build();
    }

    private void bookingIs(BookingStatus status) {
        BookingResponse booking = org.mockito.Mockito.mock(BookingResponse.class);
        when(booking.status()).thenReturn(status);
        when(bookingService.get("BKG-1")).thenReturn(booking);
    }

    @Test
    @DisplayName("Une annonce echue tombe et rend le creneau de la reservation en attente")
    void expiresAndReleasesTheHeldSlot() {
        when(repository.findExpired(any(), any())).thenReturn(List.of(overdue("BKG-1")));
        bookingIs(BookingStatus.PENDING_PAYMENT);

        CashDeclarationExpiryWorker.Sweep sweep = worker.run();

        assertThat(sweep.expired()).isEqualTo(1);
        assertThat(sweep.bookingsReleased()).isEqualTo(1);
        verify(declarationService).expire(any(), anyString());
        verify(bookingService).systemCancel(eq("BKG-1"), anyString());
        verify(notifier).expired(any());
    }

    @Test
    @DisplayName("Une reservation confirmee entre-temps n'est pas annulee · le client a pu payer autrement")
    void neverCancelsABookingAlreadySettled() {
        when(repository.findExpired(any(), any())).thenReturn(List.of(overdue("BKG-1")));
        bookingIs(BookingStatus.CONFIRMED);

        CashDeclarationExpiryWorker.Sweep sweep = worker.run();

        assertThat(sweep.expired()).isEqualTo(1);
        assertThat(sweep.bookingsReleased()).isZero();
        verify(bookingService, never()).systemCancel(anyString(), anyString());
    }

    @Test
    @DisplayName("Une annonce sur une simple facture tombe sans rien annuler · la facture reste due")
    void expiresAnInvoiceDeclarationWithoutCancellingAnything() {
        when(repository.findExpired(any(), any())).thenReturn(List.of(overdue(null)));

        CashDeclarationExpiryWorker.Sweep sweep = worker.run();

        assertThat(sweep.expired()).isEqualTo(1);
        assertThat(sweep.bookingsReleased()).isZero();
        verify(bookingService, never()).systemCancel(anyString(), anyString());
        verify(notifier).expired(any());
    }

    @Test
    @DisplayName("Une reservation qu'on ne peut pas rendre ne bloque pas l'expiration")
    void aFailingReleaseDoesNotStopTheSweep() {
        when(repository.findExpired(any(), any())).thenReturn(List.of(overdue("BKG-1")));
        when(bookingService.get("BKG-1")).thenThrow(new IllegalStateException("reservation introuvable"));

        CashDeclarationExpiryWorker.Sweep sweep = worker.run();

        assertThat(sweep.expired()).isEqualTo(1);
        assertThat(sweep.bookingsReleased()).isZero();
        verify(notifier).expired(any());
    }

    @Test
    @DisplayName("Rien d'echu, rien a faire")
    void doesNothingWhenNothingIsOverdue() {
        when(repository.findExpired(any(), any())).thenReturn(List.of());

        assertThat(worker.run().expired()).isZero();
        verify(declarationService, never()).expire(any(), anyString());
    }
}
