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
    @Mock
    private com.sni.bokaticowork.features.company.wallet.service.CompanyWalletService companyWalletService;

    @Mock
    private com.sni.bokaticowork.features.subscription.repository.BillableItemRepository billableItemRepository;

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

    // -------------------------------------------------------------------------------------
    // Portefeuille de l'organisation
    // -------------------------------------------------------------------------------------

    @Test
    void holdsOnTheCompanyWalletWhenTheMemberIsAuthorised() {
        Booking booking = booking(BookingStatus.PENDING_PAYMENT, new BigDecimal("25000"));
        booking.setBillingCompanyCode("CUS-COM-1");
        com.sni.bokaticowork.features.payment.model.WalletAccount companyWallet =
                com.sni.bokaticowork.features.payment.model.WalletAccount.builder().id(50L).walletNumber("WAL-ACME")
                        .ownerType("CUSTOMER").ownerCode("CUS-COM-1").currency("XAF").build();
        var funding = new com.sni.bokaticowork.features.company.wallet.service.CompanyWalletService.BookingFunding(
                companyWallet, 70L, "CUS-COM-1", "MEM-0001");
        when(companyWalletService.bookingFunding("MEM-0001", "CUS-COM-1", "XAF", new BigDecimal("25000")))
                .thenReturn(Optional.of(funding));
        var created = org.mockito.Mockito.mock(com.sni.bokaticowork.features.payment.dto.response.WalletHoldResponse.class);
        when(created.holdNumber()).thenReturn("WH-ACME");
        when(walletHoldService.create(any())).thenReturn(created);

        support.hold(booking);

        verify(walletHoldService).create(holdCaptor.capture());
        assertEquals("WAL-ACME", holdCaptor.getValue().walletNumber());
        verify(companyWalletService).recordBookingHold(funding, BOOKING_NUMBER, new BigDecimal("25000"), "WH-ACME");
    }

    @Test
    void settlesACompanyHoldThroughTheCompanyWallet() {
        givenActiveHold();
        givenBooking(BookingStatus.PENDING_PAYMENT);
        givenInvoice(new BigDecimal("25000"));
        when(companyWalletService.isCompanyBookingHold(HOLD_NUMBER)).thenReturn(true);

        support.settle(HOLD_NUMBER);

        verify(walletHoldService).release(HOLD_NUMBER, "SYSTEM");
        verify(companyWalletService).settleBookingHold(HOLD_NUMBER, "INV-0001", new BigDecimal("25000"));
        verify(paymentService, never()).payWithWallet(any(), any());
    }

    @Test
    void cancellingTheBookingCancelsTheCompanySpend() {
        support.release(booking(BookingStatus.CANCELLED, new BigDecimal("25000")), "annulation");

        verify(companyWalletService).cancelBookingHold(BOOKING_NUMBER, "annulation");
    }

    /**
     * Facture commune d'un lot multi-ressources : le blocage de la reservation ne regle que sa part,
     * pas le solde entier qui couvre aussi les autres reservations du membre.
     */
    @Test
    void debitsOnlyTheBookingShareOfABatchInvoice() {
        givenActiveHold();
        givenBooking(BookingStatus.CONFIRMED);
        BillingDocument invoice = new BillingDocument();
        invoice.setId(9L);
        invoice.setDocumentNumber("INV-LOT");
        invoice.setDocumentType(BillingDocumentType.INVOICE);
        invoice.setSourceCode("BI-0001,BI-0002");
        invoice.setBalanceDue(new BigDecimal("60000"));
        when(billingDocumentRepository.findFirstBySourceAndType(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(billableItemRepository.findByBillableNumber("BI-0001")).thenReturn(Optional.of(
                com.sni.bokaticowork.features.subscription.subscription.model.BillableItem.builder()
                        .billableNumber("BI-0001").invoiceId(9L).build()));
        when(billingDocumentRepository.findById(9L)).thenReturn(Optional.of(invoice));
        PaymentIntentResponse intent = org.mockito.Mockito.mock(PaymentIntentResponse.class);
        when(intent.intentNumber()).thenReturn("INT-LOT");
        when(paymentService.createIntentFromBillingDocument(any())).thenReturn(intent);

        support.settle(HOLD_NUMBER);

        ArgumentCaptor<com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentFromBillingDocumentRequest> request =
                ArgumentCaptor.forClass(com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentFromBillingDocumentRequest.class);
        verify(paymentService).createIntentFromBillingDocument(request.capture());
        assertEquals("INV-LOT", request.getValue().documentNumber());
        assertEquals(0, new BigDecimal("25000").compareTo(request.getValue().amount()));
        verify(paymentService).payWithWallet(eq("INT-LOT"), any());
    }

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
