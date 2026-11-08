package com.sni.bokaticowork.features.subscription.subscription.service.support.pass;

import com.sni.bokaticowork.features.billing.service.support.BillingTaxRuleResolver;
import com.sni.bokaticowork.features.subscription.repository.PassRenewalScheduleRepository;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingScheduleStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.PassRenewalSchedule;
import com.sni.bokaticowork.features.subscription.subscription.service.support.PlanPriceAmountCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le renouvellement d'un pass facture le prix de la periode et sa taxe, pas les frais d'entree
 * ni la caution que contient le total d'emission.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PassRenewalAmountTest {

    @Mock private PassRepository passRepository;
    @Mock private PassRenewalScheduleRepository scheduleRepository;
    @Mock private PassBillingSupport billingSupport;
    @Mock private PassEventWriter eventWriter;
    @Mock private PassPeriodCalculator periodCalculator;
    @Mock private PassPlanResolver planResolver;
    @Mock private BillingTaxRuleResolver taxRuleResolver;

    @InjectMocks
    private PassRenewalOperator operator;

    private PassRenewalSchedule schedule;

    @BeforeEach
    void setUp() {
        when(taxRuleResolver.defaultTaxProfile())
                .thenReturn(new BillingTaxRuleResolver.TaxProfile(new BigDecimal("18"), new BigDecimal("5")));
        ReflectionTestUtils.setField(operator, "priceCalculator", new PlanPriceAmountCalculator(taxRuleResolver));

        PassPlanVersion version = new PassPlanVersion();
        PassPlanPrice price = new PassPlanPrice();
        price.setAmount(new BigDecimal("10000"));
        price.setSetupFee(new BigDecimal("5000"));
        price.setDepositAmount(new BigDecimal("20000"));
        price.setTaxIncluded(false);
        when(planResolver.resolvePrice(version, "XAF")).thenReturn(price);

        // Total d'emission : 15 000 HT + 2 835 de taxes + 20 000 de caution
        Pass pass = new Pass();
        pass.setPassNumber("PASS-1");
        pass.setPassVersion(version);
        pass.setStatus(PassStatus.ACTIVE);
        pass.setCurrency("XAF");
        pass.setRenewalCount(0);
        pass.setValidFrom(Instant.parse("2026-10-01T00:00:00Z"));
        pass.setValidUntil(Instant.parse("2026-11-01T00:00:00Z"));
        pass.setSubtotalAmount(new BigDecimal("15000"));
        pass.setTaxAmount(new BigDecimal("2835"));
        pass.setTotalAmount(new BigDecimal("37835"));

        schedule = new PassRenewalSchedule();
        schedule.setPass(pass);
        schedule.setStatus(BillingScheduleStatus.ACTIVE);
        when(periodCalculator.periodEnd(any(), any(), any())).thenReturn(Instant.parse("2026-12-01T00:00:00Z"));
    }

    @Test
    void shouldBillOnlyThePeriodPriceAndItsTaxOnRenewal() {
        operator.renew(schedule);

        // 10 000 HT, TVA 1 800, centime 90
        verify(billingSupport).createAndInvoice(any(Pass.class), eq("PASS_RENEWAL"),
                argThat(amount -> amount.compareTo(new BigDecimal("11890")) == 0));
    }
}
