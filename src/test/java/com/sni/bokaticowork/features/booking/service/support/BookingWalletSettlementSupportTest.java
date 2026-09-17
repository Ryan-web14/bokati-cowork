package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.payment.dto.request.CreateWalletHoldRequest;
import com.sni.bokaticowork.features.payment.dto.request.WalletPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.enums.WalletHoldStatus;
import com.sni.bokaticowork.features.payment.enums.WalletStatus;
import com.sni.bokaticowork.features.payment.model.WalletHold;
import com.sni.bokaticowork.features.payment.repository.WalletHoldRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletHoldService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Regle du reglement au portefeuille : on bloque d'abord, on debite ensuite, et entre les deux
 * on peut encore tout rendre.
 *
 * <p>Ce qui est verifie ici est surtout ce qu'il ne faut PAS faire · ne jamais debiter deux fois,
 * ne jamais debiter une facture deja soldee, ne jamais debiter une reservation annulee, et ne
 * jamais laisser un blocage courir quand plus rien n'est du.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingWalletSettlementSupportTest {

    private static final String BOOKING_NUMBER = "BKG-0001";
    private static final String HOLD_NUMBER = "WH-0001";

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BillingDocumentRepository billingDocumentRepository;
    @Mock
    private WalletHoldRepository walletHoldRepository;
    @Mock
    private WalletHoldService walletHoldService;
    @Mock
    private WalletService walletService;
    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private BookingWalletSettlementSupport support;

    @Captor
    private ArgumentCaptor<CreateWalletHoldRequest> holdCaptor;
    @Captor
    private ArgumentCaptor<WalletPaymentRequest> paymentCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(support, "settlementDelayMinutes", 1);
        ReflectionTestUtils.setField(support, "holdGraceMinutes", 60);
        when(walletService.getOrCreate(anyString(), anyString(), anyString())).thenReturn(wallet());
        when(walletHoldRepository.findAllByStatusAndSourceTypeAndSourceCode(anyString(), anyString(), anyString()))
                .thenReturn(List.of());
    }

    // -------------------------------------------------------------------------------------
    // Blocage
    // -------------------------------------------------------------------------------------

    @Test
    void holdsTheBookingAmountOnTheOwnerWallet() {
        support.hold(booking(BookingStatus.PENDING_PAYMENT, new BigDecimal("25000")));

        verify(walletHoldService).create(holdCaptor.capture());
        CreateWalletHoldRequest request = holdCaptor.getValue();
        assertEquals("WAL-0001", request.walletNumber());
        assertEquals(new BigDecimal("25000"), request.amount());
        assertEquals("BOOKING", request.sourceType());
        assertEquals(BOOKING_NUMBER, request.sourceCode());
        // L'echeance de securite depasse largement le delai de reglement : elle ne doit jamais
        // devancer le debit, seulement rattraper un blocage orphelin.
        assertTrue(request.expiresAt().isAfter(java.time.Instant.now().plusSeconds(60L * 55)));
    }

    @Test
    void refusesTheBookingWhenTheWalletCannotCoverIt() {
        when(walletHoldService.create(any())).thenThrow(new BadRequestException("Insufficient wallet balance"));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> support.hold(booking(BookingStatus.PENDING_PAYMENT, new BigDecimal("25000"))));

        assertTrue(ex.getMessage().contains("Solde du portefeuille insuffisant"));
        assertTrue(ex.getMessage().contains("25000 XAF"));
    }

    @Test
    void doesNotStackASecondHoldOnTheSameBooking() {
        when(walletHoldRepository.findAllByStatusAndSourceTypeAndSourceCode(anyString(), anyString(), anyString()))
                .thenReturn(List.of(activeHold()));

        support.hold(booking(BookingStatus.PENDING_PAYMENT, new BigDecimal("25000")));

        verify(walletHoldService, never()).create(any());
    }

    @Test
    void reportsAPendingHoldWhileTheFundsAreBlockedButNotYetDebited() {
        when(walletHoldRepository.findAllByStatusAndSourceTypeAndSourceCode(anyString(), anyString(), anyString()))
                .thenReturn(List.of(activeHold()));

        assertTrue(support.hasPendingHold(booking(BookingStatus.CONFIRMED, new BigDecimal("25000"))));
    }

    @Test
    void reportsNoPendingHoldOnceTheDebitHasHappened() {
        // Le blocage a ete libere puis debite : plus rien ne protege l'annulation, la politique
        // d'annulation de la ressource reprend ses droits.
        assertFalse(support.hasPendingHold(booking(BookingStatus.CONFIRMED, new BigDecimal("25000"))));
    }

    @Test
    void returnsTheFundsWhenTheBookingIsCancelledWithinTheWindow() {
        when(walletHoldRepository.findAllByStatusAndSourceTypeAndSourceCode(anyString(), anyString(), anyString()))
                .thenReturn(List.of(activeHold()));

        support.release(booking(BookingStatus.CONFIRMED, new BigDecimal("25000")), "reservation annulee");

        verify(walletHoldService).release(HOLD_NUMBER, "SYSTEM");
        // Aucun mouvement comptable : ni debit, ni avoir, ni remboursement a produire.
        verifyNoInteractions(paymentService);
    }

    // -------------------------------------------------------------------------------------
    // Reglement
    // -------------------------------------------------------------------------------------

    @Test
    void releasesThenDebitsWhenTheInvoiceIsStillDue() {
        givenActiveHold();
        givenBooking(BookingStatus.PENDING_PAYMENT);
        givenInvoice(new BigDecimal("25000"));
        PaymentIntentResponse intent = org.mockito.Mockito.mock(PaymentIntentResponse.class);
        when(intent.intentNumber()).thenReturn("INT-0001");
        when(paymentService.createIntentFromBillingDocument(any())).thenReturn(intent);

        support.settle(HOLD_NUMBER);

        // Le blocage est rendu au solde disponible juste avant le prelevement, jamais apres.
        verify(walletHoldService).release(HOLD_NUMBER, "SYSTEM");
        verify(paymentService).payWithWallet(eq("INT-0001"), paymentCaptor.capture());
        assertEquals("WAL-0001", paymentCaptor.getValue().walletNumber());
    }

    @Test
    void releasesWithoutDebitingWhenTheInvoiceIsAlreadySettled() {
        givenActiveHold();
        givenBooking(BookingStatus.PENDING_PAYMENT);
        givenInvoice(BigDecimal.ZERO);

        support.settle(HOLD_NUMBER);

        verify(walletHoldService).release(HOLD_NUMBER, "SYSTEM");
        verifyNoInteractions(paymentService);
    }

    @Test
    void releasesWithoutDebitingWhenTheBookingWasCancelled() {
        givenActiveHold();
        givenBooking(BookingStatus.CANCELLED);

        support.settle(HOLD_NUMBER);

        verify(walletHoldService).release(HOLD_NUMBER, "SYSTEM");
        verifyNoInteractions(paymentService);
    }

    @Test
    void releasesWithoutDebitingWhenTheBookingNoLongerExists() {
        givenActiveHold();
        when(bookingRepository.findByBookingNumber(BOOKING_NUMBER)).thenReturn(Optional.empty());

        support.settle(HOLD_NUMBER);

        verify(walletHoldService).release(HOLD_NUMBER, "SYSTEM");
        verifyNoInteractions(paymentService);
    }

    @Test
    void ignoresAHoldThatIsNoLongerActive() {
        WalletHold captured = activeHold();
        captured.setStatus(WalletHoldStatus.CAPTURED);
        when(walletHoldRepository.findByHoldNumber(HOLD_NUMBER)).thenReturn(Optional.of(captured));

        support.settle(HOLD_NUMBER);

        verifyNoInteractions(paymentService);
        verify(walletHoldService, never()).release(anyString(), anyString());
    }

    // -------------------------------------------------------------------------------------

    private void givenActiveHold() {
        when(walletHoldRepository.findByHoldNumber(HOLD_NUMBER)).thenReturn(Optional.of(activeHold()));
    }

    private void givenBooking(BookingStatus status) {
        when(bookingRepository.findByBookingNumber(BOOKING_NUMBER))
                .thenReturn(Optional.of(booking(status, new BigDecimal("25000"))));
    }

    private void givenInvoice(BigDecimal balanceDue) {
        BillingDocument invoice = new BillingDocument();
        invoice.setDocumentNumber("INV-0001");
        invoice.setBalanceDue(balanceDue);
        when(billingDocumentRepository.findFirstBySourceAndType(
                "BILLABLE_ITEM", "BI-0001", BillingDocumentType.INVOICE.name()))
                .thenReturn(Optional.of(invoice));
    }

    private WalletHold activeHold() {
        return WalletHold.builder()
                .holdNumber(HOLD_NUMBER)
                .amount(new BigDecimal("25000"))
                .currency("XAF")
                .status(WalletHoldStatus.ACTIVE)
                .sourceType("BOOKING")
                .sourceCode(BOOKING_NUMBER)
                .build();
    }

    private Booking booking(BookingStatus status, BigDecimal amount) {
        return Booking.builder()
                .bookingNumber(BOOKING_NUMBER)
                .billableNumber("BI-0001")
                .ownerType(SubscriberType.MEMBER)
                .ownerCode("MEM-0001")
                .paymentMode(BookingPaymentMode.WALLET)
                .status(status)
                .totalAmount(amount)
                .currency("XAF")
                .startedAt(LocalDateTime.of(2026, 9, 17, 9, 0))
                .endedAt(LocalDateTime.of(2026, 9, 17, 12, 0))
                .build();
    }

    private WalletResponse wallet() {
        return new WalletResponse("WAL-0001", "MEMBER", "MEM-0001", "XAF", WalletStatus.ACTIVE,
                new BigDecimal("50000"), new BigDecimal("50000"), BigDecimal.ZERO, null, null);
    }
}
