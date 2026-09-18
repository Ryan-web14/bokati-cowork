package com.sni.bokaticowork.features.subscription.derivation.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.subscription.derivation.model.PlanDerivation;
import com.sni.bokaticowork.features.subscription.derivation.repository.PlanDerivationRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanPriceRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanVersionRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionPlan;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un lot n'abandonne personne en route.
 *
 * <p>Ce qui est protégé ici : une ligne qui échoue est rapportée et le lot continue, la
 * simulation n'écrit rien, et la protection tarifaire part de la version de catalogue courante
 * en gardant le prix ancien · l'abonné dont le prix n'a pas changé n'a rien à protéger et n'est
 * pas compté comme une erreur.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanDerivationBatchServiceTest {

    @Mock private PlanDerivationService derivationService;
    @Mock private PlanDerivationRepository derivationRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private PlanVersionRepository planVersionRepository;
    @Mock private PlanPriceRepository planPriceRepository;
    @Mock private SequenceGeneratorFacade sequenceGenerator;

    @InjectMocks
    private PlanDerivationBatchService batch;

    private SubscriptionPlan plan;
    private PlanVersion oldVersion;
    private PlanVersion newVersion;
    private Subscription first;
    private Subscription second;

    @BeforeEach
    void setUp() {
        plan = SubscriptionPlan.builder().id(1L).code("FLEX").name("Flex").build();
        oldVersion = PlanVersion.builder().id(10L).plan(plan).versionNumber(3).name("Flex").status(PlanStatus.ARCHIVED).build();
        newVersion = PlanVersion.builder().id(11L).plan(plan).versionNumber(4).name("Flex").status(PlanStatus.ACTIVE).build();
        first = Subscription.builder().id(50L).subscriptionNumber("SUB-1").planVersion(oldVersion).billingCycle(BillingCycle.MONTHLY).build();
        second = Subscription.builder().id(51L).subscriptionNumber("SUB-2").planVersion(oldVersion).billingCycle(BillingCycle.YEARLY).build();

        when(sequenceGenerator.next("plan_derivation_batch")).thenReturn("DRB-000001");
        when(subscriptionRepository.findBySubscriptionNumber("SUB-1")).thenReturn(Optional.of(first));
        when(subscriptionRepository.findBySubscriptionNumber("SUB-2")).thenReturn(Optional.of(second));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planVersionRepository.findById(10L)).thenReturn(Optional.of(oldVersion));
        when(planVersionRepository.findFirstByPlanAndStatusOrderByVersionNumberDesc(1L, "ACTIVE")).thenReturn(Optional.of(newVersion));
    }

    // ---- Lot ------------------------------------------------------------------------------------

    @Test
    void emptyBatchIsRefused() {
        assertThrows(BadRequestException.class, () -> batch.apply(List.of(), spec("90000"), true, "alice"));
    }

    @Test
    void simulationWritesNothingAndSumsTheImpact() {
        when(derivationService.simulate(eq("SUB-1"), any())).thenReturn(preview("SUB-1", "10000", false, null));
        when(derivationService.simulate(eq("SUB-2"), any())).thenReturn(preview("SUB-2", "15000", true, null));

        PlanDerivationBatchService.Outcome outcome = batch.apply(List.of("SUB-1", "SUB-2"), spec("90000"), true, "alice");

        assertNull(outcome.batchCode(), "pas de code de lot pour une simulation");
        assertEquals(2, outcome.succeeded());
        assertEquals(0, outcome.failed());
        assertEquals(1, outcome.pendingApproval());
        assertEquals(0, new BigDecimal("25000").compareTo(outcome.totalImpact()));
        verify(derivationService, never()).create(any(Subscription.class), any(), anyString(), any());
    }

    @Test
    void oneFailureDoesNotStopTheBatch() {
        when(derivationService.simulate(eq("SUB-1"), any())).thenReturn(preview("SUB-1", "0", false, "Prix plancher"));
        when(derivationService.simulate(eq("SUB-2"), any())).thenReturn(preview("SUB-2", "10000", false, null));
        PlanDerivation created = PlanDerivation.builder().derivationCode("DRV-1").status(PlanDerivation.Status.ACTIVE)
                .totalImpactAmount(new BigDecimal("10000")).build();
        when(derivationService.create(eq(second), any(), eq("alice"), eq("DRB-000001"))).thenReturn(created);

        PlanDerivationBatchService.Outcome outcome = batch.apply(List.of("SUB-1", "SUB-2"), spec("90000"), false, "alice");

        assertEquals("DRB-000001", outcome.batchCode());
        assertEquals(1, outcome.succeeded());
        assertEquals(1, outcome.failed());
        assertFalse(outcome.lines().getFirst().ok());
        assertTrue(outcome.lines().getFirst().message().contains("plancher"));
        assertEquals("DRV-1", outcome.lines().get(1).derivationCode());
        verify(derivationService, never()).create(eq(first), any(), anyString(), any());
    }

    @Test
    void anExceptionOnOneLineIsReportedNotThrown() {
        when(derivationService.simulate(eq("SUB-1"), any())).thenThrow(new ConflictException("derivation", "cet abonnement n'a pas de version de plan"));
        when(derivationService.simulate(eq("SUB-2"), any())).thenReturn(preview("SUB-2", "10000", false, null));

        PlanDerivationBatchService.Outcome outcome = batch.apply(List.of("SUB-1", "SUB-2"), spec("90000"), true, "alice");

        assertEquals(1, outcome.failed());
        assertEquals(1, outcome.succeeded());
        assertTrue(outcome.lines().getFirst().message().contains("version de plan"));
    }

    // ---- Protection tarifaire ---------------------------------------------------------------

    @Test
    void protectionStartsFromACatalogueVersion() {
        PlanVersion privateVersion = PlanVersion.builder().id(99L).plan(plan).scope(PlanVersion.Scope.SUBSCRIPTION).build();
        when(planVersionRepository.findById(99L)).thenReturn(Optional.of(privateVersion));

        assertThrows(BadRequestException.class, () -> batch.protectSubscribers(99L, null, true, "alice"));
    }

    @Test
    void protectionKeepsTheOldPriceAgainstTheCurrentCatalogue() {
        when(subscriptionRepository.findAllOpenOnPlanVersion(10L)).thenReturn(List.of(first, second));
        when(planPriceRepository.findAllByPlanVersion(10L)).thenReturn(List.of(price(oldVersion, BillingCycle.MONTHLY, "100000")));
        PlanDerivation created = PlanDerivation.builder().derivationCode("DRV-1").status(PlanDerivation.Status.ACTIVE)
                .totalImpactAmount(new BigDecimal("20000")).build();
        when(derivationService.create(eq(first), any(), eq("alice"), eq("DRB-000001"))).thenAnswer(inv -> {
            PlanDerivationService.Spec spec = inv.getArgument(1);
            assertEquals(0, new BigDecimal("100000").compareTo(spec.price()), "le prix ancien est conservé");
            assertEquals(PlanDerivation.Reason.GRANDFATHERING, spec.reason());
            assertEquals(LocalDate.of(2027, 1, 1), spec.effectiveTo());
            assertFalse(spec.promotionsAllowed());
            return created;
        });

        PlanDerivationBatchService.Outcome outcome = batch.protectSubscribers(10L, LocalDate.of(2027, 1, 1), false, "alice");

        assertEquals("DRB-000001", outcome.batchCode());
        assertEquals(2, outcome.total());
        assertEquals(1, outcome.succeeded());
        assertEquals(1, outcome.failed(), "le cycle annuel n'a pas de prix sur l'ancienne version");
        assertSame(newVersion, first.getPlanVersion(), "l'abonné est d'abord basculé sur le catalogue courant");
        assertSame(oldVersion, second.getPlanVersion(), "celui qui n'a pas été protégé n'a pas bougé");
        assertEquals(0, new BigDecimal("20000").compareTo(outcome.totalImpact()));
    }

    @Test
    void protectionSimulationDoesNotMoveAnyone() {
        when(subscriptionRepository.findAllOpenOnPlanVersion(10L)).thenReturn(List.of(first));
        when(planPriceRepository.findAllByPlanVersion(10L)).thenReturn(List.of(price(oldVersion, BillingCycle.MONTHLY, "100000")));
        when(derivationService.simulateAgainst(eq(first), eq(newVersion), any())).thenReturn(preview("SUB-1", "20000", true, null));

        PlanDerivationBatchService.Outcome outcome = batch.protectSubscribers(10L, null, true, "alice");

        assertNull(outcome.batchCode());
        assertEquals(1, outcome.succeeded());
        assertTrue(outcome.lines().getFirst().requiresApproval());
        assertSame(oldVersion, first.getPlanVersion());
        verify(subscriptionRepository, never()).save(any());
        verify(derivationService, never()).create(any(Subscription.class), any(), anyString(), any());
    }

    @Test
    void unchangedPriceIsNothingToProtectAndNotAFailure() {
        when(subscriptionRepository.findAllOpenOnPlanVersion(10L)).thenReturn(List.of(first));
        when(planPriceRepository.findAllByPlanVersion(10L)).thenReturn(List.of(price(oldVersion, BillingCycle.MONTHLY, "100000")));
        when(derivationService.simulateAgainst(eq(first), eq(newVersion), any()))
                .thenReturn(preview("SUB-1", "0", false, "Rien ne diffère du catalogue"));

        PlanDerivationBatchService.Outcome outcome = batch.protectSubscribers(10L, null, true, "alice");

        assertEquals(1, outcome.succeeded());
        assertEquals(0, outcome.failed());
        assertTrue(outcome.lines().getFirst().message().contains("rien à protéger"));
    }

    // ---- Outils -------------------------------------------------------------------------------

    private static PlanDerivationService.Spec spec(String price) {
        return new PlanDerivationService.Spec(new BigDecimal(price), null, null, null, null, null, null,
                PlanDerivation.Reason.PARTNERSHIP, null, null, null, PlanDerivation.RenewalBehaviour.KEEP, null, null);
    }

    private static PlanDerivationService.Preview preview(String number, String impact, boolean approval, String blocking) {
        return new PlanDerivationService.Preview(number, "Flex v4", new BigDecimal("100000"), new BigDecimal("90000"),
                BigDecimal.TEN, new BigDecimal(impact), "XAF", List.of(), approval, blocking);
    }

    private static PlanPrice price(PlanVersion version, BillingCycle cycle, String amount) {
        return PlanPrice.builder().id(1L).planVersion(version).billingCycle(cycle).currency("XAF").amount(new BigDecimal(amount)).build();
    }
}
