package com.sni.bokaticowork.features.subscription.promotion.pricing.service;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.subscription.promotion.pricing.dto.PricingSimulationRequest;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.TargetScope;
import com.sni.bokaticowork.features.subscription.repository.PassPlanPriceRepository;
import com.sni.bokaticowork.features.subscription.repository.PassPlanRepository;
import com.sni.bokaticowork.features.subscription.repository.PassPlanVersionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlan;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionPlanResolver;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un client qui demande le prix d'un plan n'a pas à dire dans quelle devise ce plan est vendu.
 *
 * <p>Protégé ici : la devise vient du catalogue quand elle n'est pas donnée, la devise envoyée
 * prime toujours, une référence inconnue ne fait pas échouer la simulation (elle retombe sur la
 * devise de l'établissement), et sur l'espace client l'abonné est celui de la session · ce que le
 * corps disait de lui est ignoré.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PricingRequestResolverTest {

    @Mock private SubscriptionPlanResolver planResolver;
    @Mock private PassPlanRepository passPlanRepository;
    @Mock private PassPlanVersionRepository passPlanVersionRepository;
    @Mock private PassPlanPriceRepository passPlanPriceRepository;

    @InjectMocks
    private PricingRequestResolver resolver;

    private PlanVersion planVersion;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resolver, "defaultCurrency", "XAF");
        planVersion = PlanVersion.builder().id(10L).name("Flex").build();
        when(planResolver.resolvePlanVersion(eq("PLN-COW-BOT-202606-00000000"), any())).thenReturn(planVersion);
        when(planResolver.resolvePrice(eq(planVersion), any())).thenReturn(
                PlanPrice.builder().billingCycle(BillingCycle.MONTHLY).currency("XOF").amount(new BigDecimal("200000")).build());
    }

    private static PricingSimulationRequest request(String currency, TargetScope scope, String code) {
        return new PricingSimulationRequest(null, null, null, null, null, null,
                List.of(new PricingSimulationRequest.Line("plan", scope, code, null, null, 1, new BigDecimal("200000"), null)),
                "MONTHLY", null, null, null, currency, null, null, null);
    }

    @Test
    void withoutCurrencyThePlanOfTheFirstLineDecides() {
        PricingSimulationRequest resolved = resolver.resolve(request(null, TargetScope.PLAN, "PLN-COW-BOT-202606-00000000"));

        assertEquals("XOF", resolved.currency(), "la devise du prix du plan, pas celle de l'établissement");
        assertEquals("MONTHLY", resolved.billingCycle());
        assertEquals(1, resolved.lines().size());
    }

    @Test
    void anExplicitCurrencyAlwaysWinsAndIsNormalised() {
        PricingSimulationRequest resolved = resolver.resolve(request(" eur ", TargetScope.PLAN, "PLN-COW-BOT-202606-00000000"));

        assertEquals("EUR", resolved.currency());
        verify(planResolver, never()).resolvePlanVersion(any(), any());
    }

    @Test
    void anUnknownReferenceFallsBackToTheHouseCurrencyInsteadOfFailing() {
        when(planResolver.resolvePlanVersion(eq("PLN-INCONNU"), any()))
                .thenThrow(new ResourceNotFoundException("Plan not found"));

        PricingSimulationRequest resolved = resolver.resolve(request(null, TargetScope.PLAN, "PLN-INCONNU"));

        assertEquals("XAF", resolved.currency());
    }

    @Test
    void aScopeWithoutCatalogueLookupFallsBackToo() {
        assertEquals("XAF", resolver.resolve(request(null, TargetScope.SERVICE, "SVC-DOMICILIATION")).currency());
        assertEquals("XAF", resolver.resolve(request(null, TargetScope.PLAN, null)).currency());
        assertTrue(resolver.currencyOf(request(null, TargetScope.RESOURCE, "RES-1")).isEmpty());
    }

    @Test
    void aPassLineIsLookedUpToo() {
        PassPlan plan = PassPlan.builder().code("PSP-1").build();
        PassPlanVersion version = PassPlanVersion.builder().plan(plan).build();
        when(passPlanRepository.findByCode("PSP-1")).thenReturn(Optional.of(plan));
        when(passPlanVersionRepository.findFirstByPlanAndStatusOrderByVersionNumberDesc(eq(plan), any())).thenReturn(Optional.of(version));
        when(passPlanPriceRepository.findAllByPassVersion(version)).thenReturn(List.of(
                PassPlanPrice.builder().currency("CDF").build()));

        assertEquals("CDF", resolver.resolve(request(null, TargetScope.PASS, "PSP-1")).currency());
    }

    @Test
    void onTheClientRoutesTheSessionImposesTheSubscriber() {
        PricingSimulationRequest spoofed = new PricingSimulationRequest("MEMBER", "MBR-QUELQUUN-DAUTRE", null, null, null, null,
                request(null, TargetScope.PLAN, "PLN-COW-BOT-202606-00000000").lines(),
                "MONTHLY", null, null, null, null, null, null, null);

        PricingSimulationRequest resolved = resolver.resolveFor(spoofed, "MEMBER", "MBR-2026-000012");

        assertEquals("MBR-2026-000012", resolved.subscriberCode(), "le code du corps ne doit pas l'emporter");
        assertEquals("MEMBER", resolved.subscriberType());
        assertEquals("XOF", resolved.currency());
    }

    @Test
    void nothingElseOfTheRequestIsLost() {
        PricingSimulationRequest full = new PricingSimulationRequest("CUSTOMER", "CUS-1", "GRAND_COMPTE", 2, 7, false,
                request(null, TargetScope.PLAN, "PLN-COW-BOT-202606-00000000").lines(),
                "MONTHLY", "PORTAL", "WALLET", "SITE-1", null, List.of("BIENVENUE10"), null, Boolean.FALSE);

        PricingSimulationRequest resolved = resolver.resolve(full);

        assertEquals("GRAND_COMPTE", resolved.subscriberSegment());
        assertEquals(2, resolved.kycLevel());
        assertEquals(7, resolved.tenureMonths());
        assertEquals(false, resolved.firstPurchase());
        assertEquals("PORTAL", resolved.channel());
        assertEquals("WALLET", resolved.paymentMethod());
        assertEquals("SITE-1", resolved.locationCode());
        assertEquals(List.of("BIENVENUE10"), resolved.couponCodes());
        assertEquals(Boolean.FALSE, resolved.promotionsAllowed());
    }
}
