package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.domiciliation.repository.DomiciliationContractRepository;
import com.sni.bokaticowork.features.domiciliation.repository.MailItemRepository;
import com.sni.bokaticowork.features.payment.repository.WalletHoldRepository;
import com.sni.bokaticowork.features.subscription.lifecycle.model.EarlyTerminationFormula;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionExitItem;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionPolicy;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionTermination;
import com.sni.bokaticowork.features.subscription.lifecycle.repository.SubscriptionTerminationRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionBillingSupport;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionStatusManager;
import com.sni.bokaticowork.features.subscription.subscription.service.support.subscription.SubscriptionLifecycleOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * On peut toujours résilier · mais pas n'importe quand, ni sans solder.
 *
 * <p>Protégé ici : la date d'effet respecte le préavis, les frais sont chiffrés à la demande et
 * facturés à l'acceptation seulement, le retrait rend l'abonnement actif, et l'achèvement attend
 * que la liste de sortie soit vide · un badge non rendu bloque la fermeture.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionTerminationServiceTest {

    @Mock private SubscriptionTerminationRepository terminationRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private SubscriptionCommitmentService commitmentService;
    @Mock private SubscriptionPolicyService policyService;
    @Mock private SubscriptionStatusManager statusManager;
    @Mock private SubscriptionEventWriter eventWriter;
    @Mock private SubscriptionBillingSupport billingSupport;
    @Mock private BillingDocumentRepository billingDocumentRepository;
    @Mock private WalletHoldRepository walletHoldRepository;
    @Mock private DomiciliationContractRepository domiciliationContractRepository;
    @Mock private MailItemRepository mailItemRepository;
    @Mock private SequenceGeneratorFacade sequenceGenerator;
    @Mock private OutboxService outboxService;
    @Mock private SubscriptionLifecycleOperator lifecycleOperator;

    private final ProrationCalculator proration = new ProrationCalculator();

    private SubscriptionTerminationService service;
    private Subscription subscription;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        service = new SubscriptionTerminationService(terminationRepository, subscriptionRepository, commitmentService, policyService, proration,
                statusManager, eventWriter, billingSupport, billingDocumentRepository, walletHoldRepository, domiciliationContractRepository,
                mailItemRepository, sequenceGenerator, outboxService, lifecycleOperator);
        subscription = Subscription.builder().id(1L).subscriptionNumber("SUB-1").status(SubscriptionStatus.ACTIVE)
                .subscriberType(SubscriberType.MEMBER).subscriberCode("MBR-1").billingCycle(BillingCycle.MONTHLY).currency("XAF")
                .currentPeriodStart(today.minusDays(10)).currentPeriodEnd(today.plusDays(19)).totalAmount(new BigDecimal("30000")).build();
        when(subscriptionRepository.findBySubscriptionNumber("SUB-1")).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(policyService.current()).thenReturn(SubscriptionPolicy.builder().defaultNoticeDays(30).build());
        when(commitmentService.earlyTerminationOn(any(), any())).thenReturn(SubscriptionCommitmentService.EarlyTermination.none());
        when(terminationRepository.findOpenBySubscription(1L)).thenReturn(Optional.empty());
        when(terminationRepository.save(any())).thenAnswer(inv -> {
            SubscriptionTermination t = inv.getArgument(0);
            if (t.getId() == null) t.setId(7L);
            return t;
        });
        when(sequenceGenerator.next("subscription_termination")).thenReturn("TRM-2026-00001");
        when(domiciliationContractRepository.findFirstBySubscription_IdAndStatusNotInOrderByCreatedAtDesc(anyLong(), any())).thenReturn(Optional.empty());
        when(domiciliationContractRepository.findBySubscription_Id(anyLong())).thenReturn(List.of());
        when(walletHoldRepository.findAllByStatusAndSourceTypeAndSourceCode(anyString(), anyString(), anyString())).thenReturn(List.of());
        when(billingDocumentRepository.findRecoverableDocuments(anyString(), anyString())).thenReturn(List.of());
        when(billingSupport.createStandaloneBillableItem(any(), anyString(), anyString(), any(), any(), any())).thenReturn("BIL-1");
        doAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setStatus(inv.getArgument(1));
            return null;
        }).when(statusManager).changeStatus(any(), any(), any(), any());
    }

    private static SubscriptionTerminationService.Request request(LocalDate wanted) {
        return new SubscriptionTerminationService.Request(SubscriptionTermination.ReasonCategory.RELOCATION, "déménagement", null, wanted, null);
    }

    @Test
    void effectiveDateNeverComesBeforeTheNotice() {
        SubscriptionTerminationService.Preview preview = service.preview("SUB-1", request(today.plusDays(3)));
        assertEquals(30, preview.noticePeriodDays());
        assertEquals(today.plusDays(30), preview.effectiveDate(), "la date voulue est trop tôt · le préavis l'emporte");

        preview = service.preview("SUB-1", request(today.plusDays(45)));
        assertEquals(today.plusDays(45), preview.effectiveDate(), "plus tard que le préavis, c'est le choix de l'abonné");
    }

    @Test
    void daysBeyondThePaidPeriodAreBridgedAtProrata() {
        // Periode payee jusqu'a J+19, effet a J+30 · 11 jours au tarif journalier (30000 / 30)
        SubscriptionTerminationService.Preview preview = service.preview("SUB-1", request(null));
        assertEquals(0, new BigDecimal("11000").compareTo(preview.bridgingAmount()));
    }

    @Test
    void requestPutsTheSubscriptionInNoticeWithItsChecklistAndFee() {
        when(commitmentService.earlyTerminationOn(any(), any())).thenReturn(
                new SubscriptionCommitmentService.EarlyTermination(true, 4, new BigDecimal("60000"), EarlyTerminationFormula.PERCENT_OF_REMAINING, today.plusMonths(4)));

        SubscriptionTermination t = service.request("SUB-1", request(null), "alice");

        assertEquals(SubscriptionTermination.Status.REQUESTED, t.getStatus());
        assertEquals(SubscriptionStatus.PENDING_TERMINATION, subscription.getStatus());
        assertTrue(t.getEarlyTermination());
        assertEquals(0, new BigDecimal("60000").compareTo(t.getFeeAmount()));
        assertTrue(t.getExitItems().stream().anyMatch(i -> SubscriptionExitItem.BADGE_RETURN.equals(i.getItemCode())));
        assertTrue(t.getExitItems().stream().anyMatch(i -> SubscriptionExitItem.TERMINATION_FEE.equals(i.getItemCode())));
        assertFalse(t.getExitItems().stream().anyMatch(i -> SubscriptionExitItem.MAIL_PENDING.equals(i.getItemCode())), "pas domicilié · pas de courrier");
        verify(billingSupport, never()).createStandaloneBillableItem(any(), anyString(), anyString(), any(), any(), any());
    }

    @Test
    void aSecondNoticeIsRefusedWhileOneRuns() {
        when(terminationRepository.findOpenBySubscription(1L)).thenReturn(Optional.of(SubscriptionTermination.builder().terminationCode("TRM-0").build()));
        assertThrows(ConflictException.class, () -> service.request("SUB-1", request(null), "alice"));
    }

    @Test
    void cancelledSubscriptionCannotBeTerminatedByNotice() {
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        assertThrows(ConflictException.class, () -> service.request("SUB-1", request(null), "alice"));
    }

    @Test
    void acceptBillsTheFeeAndTheBridgingDays() {
        when(commitmentService.earlyTerminationOn(any(), any())).thenReturn(
                new SubscriptionCommitmentService.EarlyTermination(true, 4, new BigDecimal("60000"), EarlyTerminationFormula.PERCENT_OF_REMAINING, today.plusMonths(4)));
        SubscriptionTermination t = service.request("SUB-1", request(null), "alice");
        when(terminationRepository.findByTerminationCode("TRM-2026-00001")).thenReturn(Optional.of(t));

        service.accept("TRM-2026-00001", "bob");

        assertEquals(SubscriptionTermination.Status.ACCEPTED, t.getStatus());
        assertEquals("bob", t.getAcceptedBy());
        verify(billingSupport).createStandaloneBillableItem(eq(subscription), eq(SubscriptionTerminationService.FEE_SOURCE), anyString(),
                eq(new BigDecimal("60000")), any(), any());
        verify(billingSupport).createStandaloneBillableItem(eq(subscription), eq(SubscriptionTerminationService.BRIDGING_SOURCE), anyString(),
                any(), any(), any());
        assertNotNull(t.getFeeBillableNumber());
        assertNotNull(t.getBridgingBillableNumber());
    }

    @Test
    void waivedFeeIsNotBilledAndItsExitLineIsClosed() {
        when(commitmentService.earlyTerminationOn(any(), any())).thenReturn(
                new SubscriptionCommitmentService.EarlyTermination(true, 4, new BigDecimal("60000"), EarlyTerminationFormula.PERCENT_OF_REMAINING, today.plusMonths(4)));
        SubscriptionTermination t = service.request("SUB-1", request(today.plusDays(19)), "alice");
        when(terminationRepository.findByTerminationCode("TRM-2026-00001")).thenReturn(Optional.of(t));

        assertThrows(BadRequestException.class, () -> service.waiveFee("TRM-2026-00001", " ", "bob"));
        service.waiveFee("TRM-2026-00001", "geste commercial", "bob");
        service.accept("TRM-2026-00001", "bob");

        assertEquals(0, BigDecimal.ZERO.compareTo(t.feeDue()));
        verify(billingSupport, never()).createStandaloneBillableItem(any(), eq(SubscriptionTerminationService.FEE_SOURCE), anyString(), any(), any(), any());
        assertEquals(SubscriptionExitItem.Status.DONE, t.getExitItems().stream()
                .filter(i -> SubscriptionExitItem.TERMINATION_FEE.equals(i.getItemCode())).findFirst().orElseThrow().getStatus());
        assertThrows(BadRequestException.class, () -> service.waiveFee("TRM-2026-00001", "trop tard", "bob"));
    }

    @Test
    void retractBringsTheSubscriptionBackToActive() {
        SubscriptionTermination t = service.request("SUB-1", request(null), "alice");
        when(terminationRepository.findByTerminationCode("TRM-2026-00001")).thenReturn(Optional.of(t));

        service.retract("TRM-2026-00001", "le client reste", "bob");

        assertEquals(SubscriptionTermination.Status.RETRACTED, t.getStatus());
        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertTrue(t.getNotes().contains("le client reste"));
    }

    @Test
    void retractAfterBillingNeedsACreditNoteFirst() {
        SubscriptionTermination t = service.request("SUB-1", request(null), "alice");
        when(terminationRepository.findByTerminationCode("TRM-2026-00001")).thenReturn(Optional.of(t));
        service.accept("TRM-2026-00001", "bob");

        assertThrows(ConflictException.class, () -> service.retract("TRM-2026-00001", "finalement non", "bob"));
    }

    @Test
    void completionWaitsForTheExitChecklist() {
        SubscriptionTermination t = service.request("SUB-1", request(null), "alice");
        when(terminationRepository.findByTerminationCode("TRM-2026-00001")).thenReturn(Optional.of(t));
        service.accept("TRM-2026-00001", "bob");

        assertFalse(service.complete(t), "le badge n'est pas rendu");
        verify(lifecycleOperator, never()).cancel(any(), any());
        assertEquals(SubscriptionExitItem.Status.DONE, t.getExitItems().stream()
                .filter(i -> SubscriptionExitItem.BALANCE_DUE.equals(i.getItemCode())).findFirst().orElseThrow().getStatus(),
                "le solde se vérifie tout seul");

        service.tick("TRM-2026-00001", SubscriptionExitItem.BADGE_RETURN, "badge 42 rendu", "bob");
        assertTrue(service.complete(t));
        assertEquals(SubscriptionTermination.Status.COMPLETED, t.getStatus());
        verify(lifecycleOperator).cancel(eq(subscription), any());
    }

    @Test
    void openInvoiceKeepsTheBalanceLineOpen() {
        SubscriptionTermination t = service.request("SUB-1", request(null), "alice");
        when(terminationRepository.findByTerminationCode("TRM-2026-00001")).thenReturn(Optional.of(t));
        service.accept("TRM-2026-00001", "bob");
        service.tick("TRM-2026-00001", SubscriptionExitItem.BADGE_RETURN, null, "bob");
        when(billingDocumentRepository.findRecoverableDocuments(anyString(), anyString())).thenReturn(List.of(new BillingDocument()));

        assertFalse(service.complete(t));
        assertThrows(ConflictException.class, () -> service.completeNow("TRM-2026-00001", "bob"));

        service.waiveItem("TRM-2026-00001", SubscriptionExitItem.BALANCE_DUE, "passé en perte", "bob");
        assertTrue(service.complete(t));
    }
}
