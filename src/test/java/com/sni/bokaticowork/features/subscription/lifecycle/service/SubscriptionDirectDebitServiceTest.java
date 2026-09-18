package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.payment.dto.response.PayInvoiceResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionDebitAttempt;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionDebitMandate;
import com.sni.bokaticowork.features.subscription.lifecycle.repository.SubscriptionDebitAttemptRepository;
import com.sni.bokaticowork.features.subscription.lifecycle.repository.SubscriptionDebitMandateRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Sans mandat, rien n'est prélevé · avec un mandat, chaque tentative est écrite.
 *
 * <p>Protégé ici : le mandat est refusé si le portefeuille n'est pas au souscripteur, un
 * prélèvement au-delà du plafond ou sans solde n'est pas tenté mais est écrit et ouvre la
 * tolérance, un succès la ferme, et trois échecs de suite suspendent le mandat.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionDirectDebitServiceTest {

    @Mock private SubscriptionDebitMandateRepository mandateRepository;
    @Mock private SubscriptionDebitAttemptRepository attemptRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private BillingDocumentRepository billingDocumentRepository;
    @Mock private WalletService walletService;
    @Mock private SubscriptionDirectDebitService.DebitGateway gateway;
    @Mock private SubscriptionGraceService graceService;
    @Mock private SubscriptionEventWriter eventWriter;
    @Mock private SequenceGeneratorFacade sequenceGenerator;
    @Mock private OutboxService outboxService;

    @InjectMocks
    private SubscriptionDirectDebitService service;

    private Subscription subscription;
    private WalletAccount wallet;
    private SubscriptionDebitMandate mandate;
    private BillingDocument invoice;

    @BeforeEach
    void setUp() {
        subscription = Subscription.builder().id(1L).subscriptionNumber("SUB-1").status(SubscriptionStatus.ACTIVE)
                .subscriberType(SubscriberType.MEMBER).subscriberCode("MBR-1").currency("XAF").build();
        wallet = WalletAccount.builder().walletNumber("WAL-1").ownerType("MEMBER").ownerCode("MBR-1").currency("XAF")
                .availableBalance(new BigDecimal("50000")).build();
        mandate = SubscriptionDebitMandate.builder().id(9L).mandateCode("MDT-000001").subscription(subscription).walletNumber("WAL-1")
                .consentGivenAt(Instant.now()).consentGivenBy("MBR-1").consentChannel(SubscriptionDebitMandate.Channel.PORTAL).build();
        invoice = new BillingDocument();
        invoice.setDocumentNumber("INV-1");
        invoice.setBalanceDue(new BigDecimal("30000"));
        invoice.setCurrency("XAF");

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.findBySubscriptionNumber("SUB-1")).thenReturn(Optional.of(subscription));
        when(walletService.serviceWallet("WAL-1")).thenReturn(wallet);
        when(billingDocumentRepository.findByDocumentNumber("INV-1")).thenReturn(Optional.of(invoice));
        when(mandateRepository.findCurrentBySubscription(1L)).thenReturn(Optional.of(mandate));
        when(mandateRepository.findByMandateCode("MDT-000001")).thenReturn(Optional.of(mandate));
        when(mandateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sequenceGenerator.next("subscription_debit_mandate")).thenReturn("MDT-000002");
    }

    @Test
    void mandateNeedsTheSubscribersOwnWallet() {
        when(mandateRepository.findCurrentBySubscription(1L)).thenReturn(Optional.empty());
        wallet.setOwnerCode("MBR-2");

        assertThrows(ConflictException.class, () -> service.give("SUB-1",
                new SubscriptionDirectDebitService.Consent("WAL-1", SubscriptionDebitMandate.Channel.PORTAL, null, null), "MBR-1"));
        assertThrows(BadRequestException.class, () -> service.give("SUB-1",
                new SubscriptionDirectDebitService.Consent("WAL-1", null, null, null), "MBR-1"));
    }

    @Test
    void oneMandateAtATime() {
        assertThrows(ConflictException.class, () -> service.give("SUB-1",
                new SubscriptionDirectDebitService.Consent("WAL-1", SubscriptionDebitMandate.Channel.PORTAL, null, null), "MBR-1"));

        when(mandateRepository.findCurrentBySubscription(1L)).thenReturn(Optional.empty());
        SubscriptionDebitMandate given = service.give("SUB-1",
                new SubscriptionDirectDebitService.Consent("WAL-1", SubscriptionDebitMandate.Channel.SIGNED_FORM, "form-12", new BigDecimal("40000")), "alice");
        assertEquals("MDT-000002", given.getMandateCode());
        assertEquals(SubscriptionDebitMandate.Status.ACTIVE, given.getStatus());
        assertEquals("form-12", given.getConsentReference());
    }

    @Test
    void withoutActiveMandateNothingIsAttempted() {
        mandate.setStatus(SubscriptionDebitMandate.Status.SUSPENDED);
        assertTrue(service.collect(1L, "INV-1").isEmpty());
        verify(gateway, never()).pay(anyString(), anyString(), any(), anyString());
    }

    @Test
    void successfulDebitClosesTheGracePeriod() {
        when(gateway.pay(eq("INV-1"), eq("WAL-1"), eq(new BigDecimal("30000")), anyString()))
                .thenReturn(new PayInvoiceResponse(null, new PaymentTransactionResponse("TXN-1", null, null, null, null, null, null, null, null, null, null, null, null)));

        SubscriptionDebitAttempt attempt = service.collect(1L, "INV-1").orElseThrow();

        assertEquals(SubscriptionDebitAttempt.Status.SUCCEEDED, attempt.getStatus());
        assertEquals("TXN-1", attempt.getTransactionNumber());
        assertEquals(0, mandate.getConsecutiveFailures());
        assertNotNull(mandate.getLastDebitAt());
        verify(graceService).exit(eq(subscription), anyString());
        verify(graceService, never()).enter(any(), anyString());
    }

    @Test
    void insufficientBalanceIsWrittenNotAttemptedAndOpensGrace() {
        wallet.setAvailableBalance(new BigDecimal("1000"));

        SubscriptionDebitAttempt attempt = service.collect(1L, "INV-1").orElseThrow();

        assertEquals(SubscriptionDebitAttempt.Status.INSUFFICIENT_FUNDS, attempt.getStatus());
        verify(gateway, never()).pay(anyString(), anyString(), any(), anyString());
        verify(graceService).enter(eq(subscription), anyString());
        assertEquals(1, mandate.getConsecutiveFailures());
    }

    @Test
    void amountBeyondTheMandateCapIsNotTaken() {
        mandate.setMaxAmountPerDebit(new BigDecimal("20000"));

        SubscriptionDebitAttempt attempt = service.collect(1L, "INV-1").orElseThrow();

        assertEquals(SubscriptionDebitAttempt.Status.OVER_LIMIT, attempt.getStatus());
        verify(gateway, never()).pay(anyString(), anyString(), any(), anyString());
    }

    @Test
    void threeFailuresInARowSuspendTheMandate() {
        wallet.setAvailableBalance(BigDecimal.ZERO);

        service.collect(1L, "INV-1");
        service.collect(1L, "INV-1");
        assertEquals(SubscriptionDebitMandate.Status.ACTIVE, mandate.getStatus());
        service.collect(1L, "INV-1");

        assertEquals(SubscriptionDebitMandate.Status.SUSPENDED, mandate.getStatus());
        assertTrue(service.collect(1L, "INV-1").isEmpty(), "suspendu · plus rien n'est tenté");

        service.reactivate("MDT-000001", "alice");
        assertEquals(SubscriptionDebitMandate.Status.ACTIVE, mandate.getStatus());
        assertEquals(0, mandate.getConsecutiveFailures());
    }

    @Test
    void paymentFailureIsRecordedAsFailedOrInsufficient() {
        doThrow(new BadRequestException("Insufficient wallet balance")).when(gateway).pay(anyString(), anyString(), any(), anyString());
        assertEquals(SubscriptionDebitAttempt.Status.INSUFFICIENT_FUNDS, service.collect(1L, "INV-1").orElseThrow().getStatus());

        doThrow(new IllegalStateException("gateway down")).when(gateway).pay(anyString(), anyString(), any(), anyString());
        SubscriptionDebitAttempt attempt = service.collect(1L, "INV-1").orElseThrow();
        assertEquals(SubscriptionDebitAttempt.Status.FAILED, attempt.getStatus());
        assertEquals("gateway down", attempt.getMessage());
    }

    @Test
    void revokedMandateStaysWritten() {
        SubscriptionDebitMandate revoked = service.revoke("MDT-000001", "le client préfère payer lui-même", "alice");
        assertEquals(SubscriptionDebitMandate.Status.REVOKED, revoked.getStatus());
        assertEquals("alice", revoked.getRevokedBy());
        assertThrows(BadRequestException.class, () -> service.revoke("MDT-000001", "encore", "alice"));
    }
}
