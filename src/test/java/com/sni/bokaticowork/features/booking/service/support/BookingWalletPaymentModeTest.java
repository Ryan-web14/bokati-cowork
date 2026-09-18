package com.sni.bokaticowork.features.booking.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingEmailService;
import com.sni.bokaticowork.features.billing.service.support.BillableItemInvoiceSupport;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingLineRepository;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Le portefeuille paie, il ne donne pas de droit.
 *
 * <p>Trois endroits l'avaient oublie. Le resolveur de contexte le refusait purement et simplement,
 * et une reservation reglee au portefeuille repondait 400 « Unsupported booking payment mode ».
 * Le pont de facturation, lui, aurait facture zero, ce qui est plus grave qu'un refus : la
 * reservation serait passee, gratuitement, sans que personne ne s'en apercoive.</p>
 */
@ExtendWith(MockitoExtension.class)
class BookingWalletPaymentModeTest {

    @Mock
    private BillableItemRepository billableItemRepository;
    @Mock
    private SequenceGeneratorFacade sequenceGenerator;
    @Mock
    private BookingLineRepository lineRepository;
    @Mock
    private BillableItemInvoiceSupport billableItemInvoiceSupport;
    @Mock
    private BillingEmailService billingEmailService;
    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private EntitlementGrantRepository grantRepository;
    @Mock
    private BookingOverageGuard overageGuard;

    @InjectMocks
    private BookingBillableBridge billableBridge;

    @Captor
    private ArgumentCaptor<BillableItem> billableCaptor;

    @Test
    void walletIsAPayableModeAndCarriesNoEntitlement() {
        assertTrue(BookingPaymentMode.WALLET.isPayable());
        assertFalse(BookingPaymentMode.WALLET.usesEntitlement());
        assertTrue(BookingPaymentMode.DIRECT.isPayable());
        assertTrue(BookingPaymentMode.SUBSCRIPTION.usesEntitlement());
        assertTrue(BookingPaymentMode.PASS.usesEntitlement());
    }

    /**
     * Payant ne veut pas dire en attente. Le portefeuille est le seul mode a la fois payant et
     * paye d'avance : les fonds sont bloques a la confirmation, donc la reservation n'attend
     * personne. Confondre les deux la laisserait en {@code PENDING_PAYMENT} pendant que l'argent
     * est deja retire du solde disponible du client.
     */
    @Test
    void onlyAnExternalPaymentLeavesTheBookingWaiting() {
        assertTrue(BookingPaymentMode.WALLET.isPrepaid());
        assertFalse(BookingPaymentMode.WALLET.awaitsPayment());

        assertFalse(BookingPaymentMode.DIRECT.isPrepaid());
        assertTrue(BookingPaymentMode.DIRECT.awaitsPayment());

        assertFalse(BookingPaymentMode.SUBSCRIPTION.awaitsPayment());
        assertFalse(BookingPaymentMode.PASS.awaitsPayment());
    }

    @Test
    void walletBookingResolvesToADirectContextWithoutLookingForAnEntitlement() {
        BookingPaymentContextResolver resolver =
                new BookingPaymentContextResolver(subscriptionRepository, grantRepository, null, overageGuard);

        BookingPaymentContextResolver.BookingPaymentContext context =
                resolver.resolve(BookingPaymentMode.WALLET, null, null);

        assertEquals(BookingPaymentContextResolver.BookingPaymentContext.direct(), context);
        verifyNoInteractions(subscriptionRepository, grantRepository);
    }

    @Test
    void walletBookingIsBilledForItsFullAmount() {
        when(sequenceGenerator.next(anyString())).thenReturn("BI-0001");
        when(billableItemRepository.save(any(BillableItem.class))).thenAnswer(call -> call.getArgument(0));
        when(lineRepository.findByBookingId(any())).thenReturn(List.of());

        billableBridge.ensureBillableItem(walletBooking(new BigDecimal("25000")));

        verify(billableItemRepository).save(billableCaptor.capture());
        assertEquals(new BigDecimal("25000"), billableCaptor.getValue().getAmount());
        // Ni libelle ni metadonnees de couverture par droit : rien n'est couvert, tout est du.
        assertEquals("Booking BKG-0001 - Salle de reunion", billableCaptor.getValue().getDescription());
        verifyNoInteractions(objectMapper);
    }

    private Booking walletBooking(BigDecimal amount) {
        return Booking.builder()
                .bookingNumber("BKG-0001")
                .resource(Resource.builder().name("Salle de reunion").build())
                .ownerType(SubscriberType.CUSTOMER)
                .ownerCode("CUS-0001")
                .paymentMode(BookingPaymentMode.WALLET)
                .totalAmount(amount)
                .currency("XAF")
                .startedAt(LocalDateTime.of(2026, 9, 17, 9, 0))
                .endedAt(LocalDateTime.of(2026, 9, 17, 12, 0))
                .build();
    }
}
