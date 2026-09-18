package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.subscription.lifecycle.model.ProrationPolicy;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionPolicy;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionStatusHistoryRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionStatusHistory;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionBillingSupport;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionStatusManager;
import com.sni.bokaticowork.features.subscription.subscription.service.support.subscription.SubscriptionLifecycleOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * La tolérance avant la suspension, et l'alignement des échéances.
 *
 * <p>Protégé ici : une échéance impayée ne coupe rien le jour même, la suspension vient au terme
 * de la tolérance et pas avant ; l'alignement ne va que vers l'avant et facture le raccordement.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionGraceAndAlignmentTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private SubscriptionStatusHistoryRepository historyRepository;
    @Mock private SubscriptionPolicyService policyService;
    @Mock private SubscriptionStatusManager statusManager;
    @Mock private SubscriptionEventWriter eventWriter;
    @Mock private OutboxService outboxService;
    @Mock private SubscriptionLifecycleOperator lifecycleOperator;
    @Mock private SubscriptionBillingSupport billingSupport;

    private SubscriptionGraceService grace;
    private SubscriptionAlignmentService alignment;
    private Subscription active;
    private Subscription inGrace;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        grace = new SubscriptionGraceService(subscriptionRepository, historyRepository, policyService, statusManager, eventWriter, outboxService, lifecycleOperator);
        alignment = new SubscriptionAlignmentService(subscriptionRepository, policyService, new ProrationCalculator(), billingSupport, eventWriter);
        active = Subscription.builder().id(1L).subscriptionNumber("SUB-1").status(SubscriptionStatus.ACTIVE).subscriberType(SubscriberType.BUSINESS_ENTITY)
                .subscriberCode("BIZ-1").billingCycle(BillingCycle.MONTHLY).currentPeriodStart(today.minusDays(9)).currentPeriodEnd(today.plusDays(20))
                .totalAmount(new BigDecimal("30000")).build();
        inGrace = Subscription.builder().id(2L).subscriptionNumber("SUB-2").status(SubscriptionStatus.GRACE_PERIOD).subscriberType(SubscriberType.BUSINESS_ENTITY)
                .subscriberCode("BIZ-1").billingCycle(BillingCycle.MONTHLY).currentPeriodStart(today.minusDays(19)).currentPeriodEnd(today.plusDays(10))
                .totalAmount(new BigDecimal("60000")).build();
        when(policyService.current()).thenReturn(SubscriptionPolicy.builder().gracePeriodDays(7).suspensionAfterGraceDays(14)
                .prorationPolicy(ProrationPolicy.DAILY).build());
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setStatus(inv.getArgument(1));
            return null;
        }).when(statusManager).changeStatus(any(), any(), any(), any());
    }

    // ---- Tolerance ------------------------------------------------------------------------------

    @Test
    void onlyEntitledSubscriptionsEnterGraceAndOnlyGraceExits() {
        assertTrue(grace.enter(active, "impayé"));
        assertEquals(SubscriptionStatus.GRACE_PERIOD, active.getStatus());
        assertFalse(grace.enter(active, "encore"), "déjà en tolérance");

        assertTrue(grace.exit(active, "payé"));
        assertEquals(SubscriptionStatus.ACTIVE, active.getStatus());
        assertFalse(grace.exit(active, "encore"));
    }

    @Test
    void sweepEntersUnpaidAndSuspendsOnlyAfterTheDelay() {
        when(subscriptionRepository.findActiveWithRenewalUnpaidSince(any())).thenReturn(List.of(active));
        when(subscriptionRepository.findAllByStatus("GRACE_PERIOD")).thenReturn(List.of(inGrace));
        SubscriptionStatusHistory recent = SubscriptionStatusHistory.builder().subscription(inGrace).toStatus(SubscriptionStatus.GRACE_PERIOD)
                .changedAt(Instant.now().minus(3, ChronoUnit.DAYS)).build();
        when(historyRepository.findAllBySubscriptionOrderByChangedAtAsc(2L)).thenReturn(List.of(recent));

        SubscriptionGraceService.Sweep sweep = grace.sweep();

        assertEquals(1, sweep.entered());
        assertEquals(0, sweep.suspended(), "3 jours de tolérance sur 14 · pas encore");
        verify(lifecycleOperator, never()).suspend(any(), any());

        recent.setChangedAt(Instant.now().minus(15, ChronoUnit.DAYS));
        sweep = grace.sweep();
        assertEquals(1, sweep.suspended());
        verify(lifecycleOperator).suspend(eq(inGrace), any());
    }

    // ---- Alignement -----------------------------------------------------------------------------

    @Test
    void alignmentMovesForwardAndBillsTheBridge() {
        when(subscriptionRepository.findOpenBySubscriber("BUSINESS_ENTITY", "BIZ-1")).thenReturn(List.of(inGrace, active));
        when(billingSupport.createStandaloneBillableItem(any(), anyString(), anyString(), any(), any(), any())).thenReturn("BIL-1");

        SubscriptionAlignmentService.Outcome simulated = alignment.align(SubscriberType.BUSINESS_ENTITY, "BIZ-1", null, true, "alice");

        assertEquals(today.plusDays(20), simulated.targetPeriodEnd(), "la plus tardive des fins de période");
        assertEquals(2, simulated.aligned());
        // SUB-2 · 10 jours de raccordement a 2 000 le jour
        assertEquals(0, new BigDecimal("20000").compareTo(simulated.totalBridging()));
        assertEquals(today.plusDays(10), inGrace.getCurrentPeriodEnd(), "la simulation ne bouge rien");
        verify(billingSupport, never()).createStandaloneBillableItem(any(), anyString(), anyString(), any(), any(), any());

        SubscriptionAlignmentService.Outcome done = alignment.align(SubscriberType.BUSINESS_ENTITY, "BIZ-1", null, false, "alice");

        assertEquals(today.plusDays(20), inGrace.getCurrentPeriodEnd());
        assertEquals(today.plusDays(21), inGrace.getNextBillingDate());
        assertEquals(today.plusDays(20), active.getCurrentPeriodEnd());
        verify(billingSupport).createStandaloneBillableItem(eq(inGrace), eq(SubscriptionAlignmentService.BRIDGING_SOURCE), anyString(),
                eq(new BigDecimal("20000.0000")), eq(today.plusDays(11)), eq(today.plusDays(20)));
        assertEquals(1, done.lines().stream().filter(l -> "déjà alignée".equals(l.message())).count());
    }

    @Test
    void alignmentNeverShortensAPeriod() {
        when(subscriptionRepository.findOpenBySubscriber("BUSINESS_ENTITY", "BIZ-1")).thenReturn(List.of(inGrace, active));

        SubscriptionAlignmentService.Outcome outcome = alignment.align(SubscriberType.BUSINESS_ENTITY, "BIZ-1", today.plusDays(10), false, "alice");

        assertEquals(1, outcome.skipped());
        assertEquals(today.plusDays(20), active.getCurrentPeriodEnd(), "on ne rend pas d'argent · on laisse");
        assertTrue(outcome.lines().stream().anyMatch(l -> !l.aligned() && l.message().contains("rendre de l'argent")));
    }

    @Test
    void alignmentNeedsTwoSubscriptions() {
        when(subscriptionRepository.findOpenBySubscriber("BUSINESS_ENTITY", "BIZ-1")).thenReturn(List.of(active));
        assertThrows(BadRequestException.class, () -> alignment.align(SubscriberType.BUSINESS_ENTITY, "BIZ-1", null, true, "alice"));
    }
}
