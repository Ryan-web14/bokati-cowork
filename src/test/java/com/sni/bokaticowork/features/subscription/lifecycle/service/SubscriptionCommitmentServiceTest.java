package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.subscription.lifecycle.model.EarlyTerminationFormula;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionCommitment;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionPolicy;
import com.sni.bokaticowork.features.subscription.lifecycle.repository.SubscriptionCommitmentRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L'engagement ne dit pas « on ne peut pas partir » · il dit ce que cela coûte.
 *
 * <p>Protégé ici : la formule vient de la politique quand l'engagement n'en fixe pas, tout mois
 * entamé compte, et l'engagement posé par le plan ne remplace jamais un engagement déjà posé.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionCommitmentServiceTest {

    @Mock private SubscriptionCommitmentRepository commitmentRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private SubscriptionPolicyService policyService;
    @Mock private SubscriptionEventWriter eventWriter;

    @InjectMocks
    private SubscriptionCommitmentService service;

    private Subscription subscription;
    private SubscriptionPolicy policy;

    @BeforeEach
    void setUp() {
        subscription = Subscription.builder().id(1L).subscriptionNumber("SUB-1").status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 1, 1)).billingCycle(BillingCycle.MONTHLY).totalAmount(new BigDecimal("100000")).build();
        policy = SubscriptionPolicy.builder().earlyTerminationFormula(EarlyTerminationFormula.PERCENT_OF_REMAINING)
                .earlyTerminationPercent(new BigDecimal("50")).build();
        when(policyService.current()).thenReturn(policy);
        when(commitmentRepository.findBySubscription_Id(1L)).thenReturn(Optional.empty());
        when(commitmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void setTakesTheFormulaFromThePolicyWhenNoneIsGiven() {
        SubscriptionCommitment c = service.set(subscription, new SubscriptionCommitmentService.Spec(12, null, null, null, null, null),
                SubscriptionCommitment.Source.MANUAL, "alice");

        assertEquals(LocalDate.of(2026, 1, 1), c.getCommitmentStart());
        assertEquals(LocalDate.of(2026, 12, 31), c.getCommitmentEnd());
        assertEquals(EarlyTerminationFormula.PERCENT_OF_REMAINING, c.getEarlyTerminationFormula());
        assertEquals(0, new BigDecimal("50").compareTo(c.getEarlyTerminationPercent()));
    }

    @Test
    void fixedFeeNeedsAnAmountSomewhere() {
        assertThrows(BadRequestException.class, () -> service.set(subscription,
                new SubscriptionCommitmentService.Spec(6, null, EarlyTerminationFormula.FIXED_FEE, null, null, null),
                SubscriptionCommitment.Source.MANUAL, "alice"));
        assertThrows(BadRequestException.class, () -> service.set(subscription,
                new SubscriptionCommitmentService.Spec(0, null, null, null, null, null), SubscriptionCommitment.Source.MANUAL, "alice"));
    }

    @Test
    void planCommitmentDoesNotOverrideAnExistingOne() {
        SubscriptionCommitment existing = SubscriptionCommitment.builder().subscription(subscription).commitmentMonths(24)
                .commitmentStart(LocalDate.of(2026, 1, 1)).commitmentEnd(LocalDate.of(2027, 12, 31))
                .earlyTerminationFormula(EarlyTerminationFormula.NONE).build();
        when(commitmentRepository.findBySubscription_Id(1L)).thenReturn(Optional.of(existing));

        assertSame(existing, service.setFromPlan(subscription, 12).orElseThrow());
        assertTrue(service.setFromPlan(subscription, 0).isEmpty());
        verify(commitmentRepository, never()).save(any());
    }

    @Test
    void everyStartedMonthCounts() {
        assertEquals(0, SubscriptionCommitmentService.remainingMonths(LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31)));
        assertEquals(1, SubscriptionCommitmentService.remainingMonths(LocalDate.of(2026, 12, 15), LocalDate.of(2026, 12, 31)));
        assertEquals(6, SubscriptionCommitmentService.remainingMonths(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31)));
        assertEquals(7, SubscriptionCommitmentService.remainingMonths(LocalDate.of(2026, 6, 15), LocalDate.of(2026, 12, 31)));
    }

    @Test
    void feeFollowsTheFormula() {
        SubscriptionCommitment c = SubscriptionCommitment.builder().subscription(subscription).commitmentMonths(12)
                .commitmentStart(LocalDate.of(2026, 1, 1)).commitmentEnd(LocalDate.of(2026, 12, 31))
                .earlyTerminationFormula(EarlyTerminationFormula.PERCENT_OF_REMAINING).earlyTerminationPercent(new BigDecimal("50")).build();
        when(commitmentRepository.findBySubscription_Id(1L)).thenReturn(Optional.of(c));
        LocalDate on = LocalDate.of(2026, 7, 1);

        SubscriptionCommitmentService.EarlyTermination early = service.earlyTerminationOn(subscription, on);
        assertTrue(early.early());
        assertEquals(6, early.remainingMonths());
        assertEquals(0, new BigDecimal("300000").compareTo(early.fee()), "6 mois × 100 000 × 50 %");

        c.setEarlyTerminationFormula(EarlyTerminationFormula.REMAINING_PERIODS);
        assertEquals(0, new BigDecimal("600000").compareTo(service.earlyTerminationOn(subscription, on).fee()));

        c.setEarlyTerminationFormula(EarlyTerminationFormula.FIXED_FEE);
        c.setEarlyTerminationFixedFee(new BigDecimal("25000"));
        assertEquals(0, new BigDecimal("25000").compareTo(service.earlyTerminationOn(subscription, on).fee()));

        c.setEarlyTerminationFormula(EarlyTerminationFormula.NONE);
        assertEquals(0, BigDecimal.ZERO.compareTo(service.earlyTerminationOn(subscription, on).fee()));

        assertFalse(service.earlyTerminationOn(subscription, LocalDate.of(2027, 1, 1)).early(), "après le terme, rien n'est dû");
    }

    @Test
    void yearlyPriceIsBroughtBackToAMonth() {
        subscription.setBillingCycle(BillingCycle.YEARLY);
        subscription.setTotalAmount(new BigDecimal("1200000"));
        assertEquals(0, new BigDecimal("100000").compareTo(SubscriptionCommitmentService.monthlyEquivalent(subscription)));
        subscription.setBillingCycle(BillingCycle.QUARTERLY);
        subscription.setTotalAmount(new BigDecimal("300000"));
        assertEquals(0, new BigDecimal("100000").compareTo(SubscriptionCommitmentService.monthlyEquivalent(subscription)));
    }

    @Test
    void endedCommitmentRollsOverOnlyWhenItSaidSo() {
        SubscriptionCommitment rolling = SubscriptionCommitment.builder().subscription(subscription).commitmentMonths(12)
                .commitmentStart(LocalDate.of(2025, 1, 1)).commitmentEnd(LocalDate.of(2025, 12, 31))
                .earlyTerminationFormula(EarlyTerminationFormula.NONE).autoRenewCommitment(true).build();
        Subscription other = Subscription.builder().id(2L).subscriptionNumber("SUB-2").status(SubscriptionStatus.ACTIVE).build();
        SubscriptionCommitment falling = SubscriptionCommitment.builder().subscription(other).commitmentMonths(12)
                .commitmentStart(LocalDate.of(2025, 1, 1)).commitmentEnd(LocalDate.of(2025, 12, 31))
                .earlyTerminationFormula(EarlyTerminationFormula.NONE).autoRenewCommitment(false).build();
        when(commitmentRepository.findAllEndedBefore(any())).thenReturn(List.of(rolling, falling));

        assertEquals(1, service.rollOverEnded());

        assertEquals(LocalDate.of(2026, 1, 1), rolling.getCommitmentStart());
        assertEquals(LocalDate.of(2026, 12, 31), rolling.getCommitmentEnd());
        verify(commitmentRepository).delete(falling);
    }
}
