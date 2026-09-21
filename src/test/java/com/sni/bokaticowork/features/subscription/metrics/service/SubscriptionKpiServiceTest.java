package com.sni.bokaticowork.features.subscription.metrics.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Le revenu récurrent ramène tous les rythmes au mois · un annuel ne pèse pas zéro. */
class SubscriptionKpiServiceTest {

    @Test
    void everyCycleIsBroughtBackToAMonth() {
        assertEquals(0, new BigDecimal("100000").compareTo(SubscriptionKpiService.monthly("MONTHLY", new BigDecimal("100000"))));
        assertEquals(0, new BigDecimal("100000").compareTo(SubscriptionKpiService.monthly("YEARLY", new BigDecimal("1200000"))));
        assertEquals(0, new BigDecimal("100000").compareTo(SubscriptionKpiService.monthly("QUARTERLY", new BigDecimal("300000"))));
        assertEquals(0, new BigDecimal("100000").compareTo(SubscriptionKpiService.monthly("unknown", new BigDecimal("100000"))));
    }
}
