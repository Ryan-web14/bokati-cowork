package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionFreeze;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionPolicy;
import com.sni.bokaticowork.features.subscription.lifecycle.repository.SubscriptionFreezeRepository;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionBillingSupport;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Un gel sans bornes est un moyen de ne plus payer sans résilier · ici il en a. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionFreezeServiceTest {

    @Mock private SubscriptionFreezeRepository freezeRepository;
    @Mock private SubscriptionPolicyService policyService;
    @Mock private SubscriptionBillingSupport billingSupport;
    @Mock private SubscriptionEventWriter eventWriter;

    @InjectMocks
    private SubscriptionFreezeService service;

    private Subscription subscription;
    private SubscriptionPolicy policy;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        subscription = Subscription.builder().id(1L).subscriptionNumber("SUB-1").currentPeriodStart(today.minusDays(9))
                .currentPeriodEnd(today.plusDays(20)).totalAmount(new BigDecimal("30000")).build();
        policy = SubscriptionPolicy.builder().freezeMaxPerYear(2).freezeMaxDays(30).freezeFeePercent(BigDecimal.ZERO).build();
        when(policyService.current()).thenReturn(policy);
        when(freezeRepository.findBySubscriptionSince(anyLong(), any())).thenReturn(List.of());
        when(freezeRepository.findOpenBySubscription(1L)).thenReturn(Optional.empty());
        when(freezeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(billingSupport.createStandaloneBillableItem(any(), anyString(), anyString(), any(), any(), any())).thenReturn("BIL-9");
    }

    @Test
    void durationAndQuotaAreEnforced() {
        assertThrows(ConflictException.class, () -> service.start(subscription, today.plusDays(31), null, "alice"));
        assertThrows(BadRequestException.class, () -> service.start(subscription, today, null, "alice"));

        when(freezeRepository.findBySubscriptionSince(anyLong(), any())).thenReturn(List.of(new SubscriptionFreeze(), new SubscriptionFreeze()));
        assertThrows(ConflictException.class, () -> service.start(subscription, today.plusDays(10), null, "alice"));
        assertFalse(service.allowance(subscription).canFreeze());
    }

    @Test
    void freeFreezeBillsNothing() {
        SubscriptionFreeze freeze = service.start(subscription, today.plusDays(10), "vacances", "alice");

        assertEquals(10, freeze.getDaysPlanned());
        assertEquals(0, BigDecimal.ZERO.compareTo(freeze.getFeeAmount()));
        verify(billingSupport, never()).createStandaloneBillableItem(any(), anyString(), anyString(), any(), any(), any());
    }

    @Test
    void feePercentIsBilledOnTheFrozenDays() {
        policy.setFreezeFeePercent(new BigDecimal("50"));

        SubscriptionFreeze freeze = service.start(subscription, today.plusDays(10), null, "alice");

        // 30 000 sur 30 jours · 1 000 le jour · 10 jours · 50 %
        assertEquals(0, new BigDecimal("5000").compareTo(freeze.getFeeAmount()));
        assertEquals("BIL-9", freeze.getFeeBillableNumber());
        verify(billingSupport).createStandaloneBillableItem(eq(subscription), eq(SubscriptionFreezeService.FEE_SOURCE), anyString(),
                eq(new BigDecimal("5000.0000")), eq(today), eq(today.plusDays(9)));
    }

    @Test
    void resumingClosesTheOpenFreezeWithEffectiveDays() {
        SubscriptionFreeze open = SubscriptionFreeze.builder().subscription(subscription).startedOn(today.minusDays(4))
                .plannedUntil(today.plusDays(6)).daysPlanned(10).build();
        when(freezeRepository.findOpenBySubscription(1L)).thenReturn(Optional.of(open));

        service.end(subscription, today);

        assertEquals(today, open.getResumedOn());
        assertEquals(4, open.getDaysEffective());
    }
}
