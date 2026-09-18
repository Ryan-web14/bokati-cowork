package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.subscription.change.model.SubscriptionChangeRequest;
import com.sni.bokaticowork.features.subscription.lifecycle.model.ProrationPolicy;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionPolicy;
import com.sni.bokaticowork.features.subscription.repository.PlanPriceRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.PlanPriceAmountCalculator;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionBillingSupport;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Le changement de plan en cours de période rend ce qui n'est pas consommé et fait payer ce qui l'est.
 *
 * <p>Protégé ici : la différence est facturée à la hausse, créditée au portefeuille à la baisse,
 * et rien ne bouge avec la politique NONE · le nouveau prix vaut à la prochaine échéance.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanChangeProrationServiceTest {

    @Mock private SubscriptionPolicyService policyService;
    @Mock private PlanPriceRepository planPriceRepository;
    @Mock private PlanPriceAmountCalculator amountCalculator;
    @Mock private SubscriptionBillingSupport billingSupport;
    @Mock private WalletService walletService;
    @Mock private SubscriptionEventWriter eventWriter;

    private PlanChangeProrationService service;
    private Subscription subscription;
    private PlanVersion target;
    private SubscriptionPolicy policy;

    @BeforeEach
    void setUp() {
        service = new PlanChangeProrationService(policyService, new ProrationCalculator(), planPriceRepository, amountCalculator,
                billingSupport, walletService, eventWriter);
        subscription = Subscription.builder().id(1L).subscriptionNumber("SUB-1").subscriberType(SubscriberType.MEMBER).subscriberCode("MBR-1")
                .billingCycle(BillingCycle.MONTHLY).currency("XAF")
                .currentPeriodStart(LocalDate.of(2026, 3, 1)).currentPeriodEnd(LocalDate.of(2026, 3, 30))
                .subtotalAmount(new BigDecimal("30000")).taxAmount(BigDecimal.ZERO).totalAmount(new BigDecimal("30000")).build();
        target = PlanVersion.builder().id(20L).name("Pro").build();
        policy = SubscriptionPolicy.builder().prorationPolicy(ProrationPolicy.DAILY).build();
        when(policyService.current()).thenReturn(policy);
        when(planPriceRepository.findAllByPlanVersion(20L)).thenReturn(List.of(
                PlanPrice.builder().planVersion(target).billingCycle(BillingCycle.MONTHLY).currency("XAF").amount(new BigDecimal("60000")).taxIncluded(true).build()));
        when(amountCalculator.compute(any(), any(), any(), any())).thenAnswer(inv -> {
            BigDecimal amount = inv.getArgument(0);
            return new PlanPriceAmountCalculator.Amounts(amount, BigDecimal.ZERO, amount);
        });
        when(billingSupport.createStandaloneBillableItem(any(), anyString(), anyString(), any(), any(), any())).thenReturn("BIL-1");
        when(walletService.getOrCreate("MEMBER", "MBR-1", "XAF")).thenReturn(new WalletResponse("WAL-1", null, null, null, null, null, null, null, null, null));
        when(walletService.serviceWallet("WAL-1")).thenReturn(WalletAccount.builder().walletNumber("WAL-1").build());
    }

    private SubscriptionChangeRequest change(LocalDate effective) {
        return SubscriptionChangeRequest.builder().changeNumber("CHG-1").subscription(subscription).targetPlanVersion(target).effectiveDate(effective).build();
    }

    @Test
    void upgradeMidPeriodBillsTheDifferenceOnRemainingDays() {
        // Effet le 21 · 10 jours restants sur 30 · 10 000 non consommes a 30 000, 20 000 dus a 60 000
        SubscriptionChangeRequest change = change(LocalDate.of(2026, 3, 21));

        PlanChangeProrationService.Preview result = service.apply(change);

        assertFalse(result.credit());
        assertEquals(0, new BigDecimal("10000").compareTo(result.amount()));
        assertEquals(ProrationPolicy.DAILY, change.getProrationPolicy());
        assertEquals("BIL-1", change.getProrationBillableNumber());
        assertSame(target, subscription.getPlanVersion());
        assertEquals(0, new BigDecimal("60000").compareTo(subscription.getTotalAmount()));
        verify(walletService, never()).credit(any(), any(), any(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void downgradeMidPeriodCreditsTheWallet() {
        subscription.setTotalAmount(new BigDecimal("90000"));
        SubscriptionChangeRequest change = change(LocalDate.of(2026, 3, 21));

        PlanChangeProrationService.Preview result = service.apply(change);

        assertTrue(result.credit());
        assertEquals(0, new BigDecimal("10000").compareTo(result.amount()), "30 000 non consommés, 20 000 dus");
        assertTrue(change.getProrationCredit());
        verify(walletService).credit(any(), eq(new BigDecimal("10000.0000")), eq(WalletEntryType.ADJUSTMENT), eq("PLAN_CHANGE"), eq("CHG-1"),
                anyString(), anyString(), eq("PLAN_CHANGE_CREDIT:CHG-1"));
        verify(billingSupport, never()).createStandaloneBillableItem(any(), anyString(), anyString(), any(), any(), any());
    }

    @Test
    void policyNoneMovesNoMoneyUntilNextPeriod() {
        policy.setProrationPolicy(ProrationPolicy.NONE);
        SubscriptionChangeRequest change = change(LocalDate.of(2026, 3, 21));

        PlanChangeProrationService.Preview result = service.apply(change);

        assertEquals(0, BigDecimal.ZERO.compareTo(result.amount()));
        assertSame(target, subscription.getPlanVersion());
        verify(billingSupport, never()).createStandaloneBillableItem(any(), anyString(), anyString(), any(), any(), any());
        verify(walletService, never()).credit(any(), any(), any(), anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
