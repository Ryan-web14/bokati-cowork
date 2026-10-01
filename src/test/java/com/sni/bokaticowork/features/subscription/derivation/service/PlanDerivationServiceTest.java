package com.sni.bokaticowork.features.subscription.derivation.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.subscription.derivation.model.PlanDerivation;
import com.sni.bokaticowork.features.subscription.derivation.model.PlanDerivationApprovalRule;
import com.sni.bokaticowork.features.subscription.derivation.model.PlanDerivationDelta;
import com.sni.bokaticowork.features.subscription.derivation.repository.PlanDerivationApprovalRuleRepository;
import com.sni.bokaticowork.features.subscription.derivation.repository.PlanDerivationDeltaRepository;
import com.sni.bokaticowork.features.subscription.derivation.repository.PlanDerivationRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanBenefitRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanEntitlementRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanPriceRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanVersionRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionPlan;
import com.sni.bokaticowork.features.subscription.subscription.service.support.PlanPriceAmountCalculator;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un abonnement peut avoir son propre prix · sans que le catalogue le sache.
 *
 * <p>Ce qui est protégé ici : la version privée descend toujours d'une version de catalogue, le
 * plancher est infranchissable même avec un visa, le visa vient d'une autre personne que le
 * demandeur, et le renouvellement rend l'abonnement au catalogue quand la dérivation l'a décidé
 * d'avance. Si l'un de ces points cède, une négociation devient une remise permanente que plus
 * personne ne surveille.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanDerivationServiceTest {

    @Mock private PlanDerivationRepository derivationRepository;
    @Mock private PlanDerivationDeltaRepository deltaRepository;
    @Mock private PlanDerivationApprovalRuleRepository approvalRuleRepository;
    @Mock private PlanVersionRepository planVersionRepository;
    @Mock private PlanPriceRepository planPriceRepository;
    @Mock private PlanEntitlementRepository planEntitlementRepository;
    @Mock private PlanBenefitRepository planBenefitRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private PlanPriceAmountCalculator amountCalculator;
    @Mock private SequenceGeneratorFacade sequenceGenerator;

    @InjectMocks
    private PlanDerivationService service;

    private SubscriptionPlan plan;
    private PlanVersion catalogue;
    private Subscription subscription;
    private PlanDerivationApprovalRule rule;
    private final List<PlanVersion> savedVersions = new ArrayList<>();
    private final List<PlanPrice> savedPrices = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong(100);

    @BeforeEach
    void setUp() {
        plan = SubscriptionPlan.builder().id(1L).code("FLEX").name("Flex").build();
        catalogue = PlanVersion.builder().id(10L).plan(plan).versionNumber(3).name("Flex").status(PlanStatus.ACTIVE)
                .scope(PlanVersion.Scope.CATALOGUE).build();
        subscription = Subscription.builder().id(50L).subscriptionNumber("SUB-2026-000001").planVersion(catalogue)
                .billingCycle(BillingCycle.MONTHLY).currency("XAF").build();
        rule = PlanDerivationApprovalRule.builder().id(1L).ruleCode("PDR-DEFAULT").name("défaut")
                .maxDiscountPercentWithoutApproval(BigDecimal.TEN)
                .maxImpactWithoutApproval(new BigDecimal("50000"))
                .maxDiscountPercentAllowed(new BigDecimal("50")).build();

        when(subscriptionRepository.findBySubscriptionNumber("SUB-2026-000001")).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(approvalRuleRepository.findFirstByActiveTrueOrderByIdAsc()).thenReturn(Optional.of(rule));
        when(planPriceRepository.findAllByPlanVersion(10L)).thenReturn(List.of(price(catalogue, "100000")));
        when(planPriceRepository.findAllByPlanVersion(anyLong())).thenAnswer(inv -> {
            Long versionId = inv.getArgument(0);
            if (versionId == 10L) return List.of(price(catalogue, "100000"));
            return savedPrices.stream().filter(p -> p.getPlanVersion().getId().equals(versionId)).toList();
        });
        when(planPriceRepository.save(any())).thenAnswer(inv -> {
            PlanPrice p = inv.getArgument(0);
            savedPrices.add(p);
            return p;
        });
        when(planEntitlementRepository.findAllByPlanVersionOrderByPriorityAsc(anyLong())).thenReturn(List.of());
        when(planBenefitRepository.findAllByPlanVersionOrderByDisplayOrderAscCreatedAtAsc(anyLong())).thenReturn(List.of());
        when(planVersionRepository.maxVersionNumber(1L)).thenReturn(3);
        when(planVersionRepository.findById(10L)).thenReturn(Optional.of(catalogue));
        when(planVersionRepository.findFirstByPlanAndStatusOrderByVersionNumberDesc(1L, "ACTIVE")).thenReturn(Optional.of(catalogue));
        when(planVersionRepository.save(any())).thenAnswer(inv -> {
            PlanVersion v = inv.getArgument(0);
            if (v.getId() == null) v.setId(ids.incrementAndGet());
            savedVersions.add(v);
            return v;
        });
        when(derivationRepository.save(any())).thenAnswer(inv -> {
            PlanDerivation d = inv.getArgument(0);
            if (d.getId() == null) d.setId(ids.incrementAndGet());
            if (d.getPeriodsApplied() == null) d.setPeriodsApplied(0);
            return d;
        });
        when(deltaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(derivationRepository.findFirstBySubscription_IdAndStatus(50L, PlanDerivation.Status.ACTIVE)).thenReturn(Optional.empty());
        when(sequenceGenerator.next("plan_derivation")).thenReturn("DRV-2026-000001", "DRV-2026-000002");
        when(amountCalculator.compute(any(), any(), any(), any())).thenAnswer(inv -> {
            BigDecimal amount = inv.getArgument(0);
            return new PlanPriceAmountCalculator.Amounts(amount, BigDecimal.ZERO, amount);
        });
    }

    // ---- Simulation ---------------------------------------------------------------------------

    @Test
    void simulateReportsTheGapAndWhetherAVisaIsNeeded() {
        PlanDerivationService.Preview preview = service.simulate("SUB-2026-000001", spec("80000", null));

        assertTrue(preview.allowed());
        assertEquals(0, new BigDecimal("20").compareTo(preview.discountPercent()));
        assertEquals(0, new BigDecimal("20000").compareTo(preview.totalImpact()));
        assertTrue(preview.requiresApproval(), "20 % dépasse les 10 % sans visa");
        assertEquals(1, preview.deltas().size());
        assertEquals(PlanDerivationDelta.Type.PRICE, preview.deltas().getFirst().type());
    }

    @Test
    void simulateWithoutAnyDifferenceHasNoObject() {
        PlanDerivationService.Preview preview = service.simulate("SUB-2026-000001", spec("100000", null));

        assertFalse(preview.allowed());
        assertTrue(preview.blockingReason().startsWith("Rien ne diffère"));
    }

    @Test
    void floorPriceBlocksEvenWhenTheDiscountWouldOnlyNeedAVisa() {
        catalogue.setFloorPrice(new BigDecimal("90000"));

        PlanDerivationService.Preview preview = service.simulate("SUB-2026-000001", spec("85000", null));

        assertFalse(preview.allowed());
        assertTrue(preview.blockingReason().contains("plancher"));
        assertThrows(ConflictException.class, () -> service.create("SUB-2026-000001", spec("85000", null), "alice"));
        verify(derivationRepository, never()).save(any());
    }

    @Test
    void discountBeyondTheAdmittedMaximumIsRefused() {
        PlanDerivationService.Preview preview = service.simulate("SUB-2026-000001", spec("40000", null));

        assertFalse(preview.allowed());
        assertTrue(preview.blockingReason().contains("maximum admis"));
    }

    // ---- Creation -----------------------------------------------------------------------------

    @Test
    void smallDiscountIsAppliedImmediatelyOnAPrivateVersion() {
        PlanDerivation created = service.create("SUB-2026-000001", spec("95000", null), "alice");

        assertEquals(PlanDerivation.Status.ACTIVE, created.getStatus());
        assertNotNull(created.getAppliedAt());
        PlanVersion derived = created.getDerivedPlanVersion();
        assertEquals(PlanVersion.Scope.SUBSCRIPTION, derived.getScope());
        assertEquals(50L, derived.getOwnerSubscriptionId());
        assertEquals(10L, derived.getDerivedFromVersionId());
        assertEquals(4, derived.getVersionNumber());
        assertEquals(PlanStatus.ACTIVE, derived.getStatus());
        assertSame(derived, subscription.getPlanVersion(), "l'abonnement lit désormais sa version privée");
        assertEquals(0, new BigDecimal("95000").compareTo(subscription.getTotalAmount()));
        assertEquals(0, new BigDecimal("95000").compareTo(savedPrices.getFirst().getAmount()));
        assertFalse(created.getPromotionsAllowed());
        assertFalse(service.promotionsAllowed(subscriptionWithActive(created)));
    }

    @Test
    void largerDiscountWaitsForAVisaAndTheVersionStaysDraft() {
        PlanDerivation created = service.create("SUB-2026-000001", spec("80000", null), "alice");

        assertEquals(PlanDerivation.Status.PENDING_APPROVAL, created.getStatus());
        assertNull(created.getAppliedAt());
        assertEquals(PlanStatus.DRAFT, created.getDerivedPlanVersion().getStatus());
        assertSame(catalogue, subscription.getPlanVersion(), "rien ne bouge tant que le visa n'est pas posé");
    }

    @Test
    void aDerivationNeedsAReason() {
        PlanDerivationService.Spec noReason = new PlanDerivationService.Spec(new BigDecimal("95000"), null, null, null, null, null, null,
                null, null, null, null, null, null, null);

        assertThrows(BadRequestException.class, () -> service.create("SUB-2026-000001", noReason, "alice"));
    }

    @Test
    void revertAfterPeriodsNeedsTheNumberOfPeriods() {
        PlanDerivationService.Spec spec = new PlanDerivationService.Spec(new BigDecimal("95000"), null, null, null, null, null, null,
                PlanDerivation.Reason.NEGOTIATION, null, null, null, PlanDerivation.RenewalBehaviour.REVERT_AFTER_PERIODS, null, null);

        assertThrows(BadRequestException.class, () -> service.create("SUB-2026-000001", spec, "alice"));
    }

    @Test
    void newDerivationSupersedesTheActiveOne() {
        PlanDerivation first = service.create("SUB-2026-000001", spec("95000", null), "alice");
        when(derivationRepository.findFirstBySubscription_IdAndStatus(50L, PlanDerivation.Status.ACTIVE)).thenReturn(Optional.of(first));
        when(planVersionRepository.findById(first.getDerivedPlanVersion().getId())).thenReturn(Optional.of(first.getDerivedPlanVersion()));
        when(planVersionRepository.maxVersionNumber(1L)).thenReturn(4);

        PlanDerivation second = service.create("SUB-2026-000001", spec("92000", null), "alice");

        assertEquals(PlanDerivation.Status.SUPERSEDED, first.getStatus());
        assertNotNull(first.getEndedAt());
        assertEquals(PlanStatus.ARCHIVED, first.getDerivedPlanVersion().getStatus());
        assertEquals(first.getId(), second.getSupersedesDerivationId());
        assertSame(catalogue, second.getSourcePlanVersion(), "la seconde part du catalogue, pas de la première version privée");
        assertEquals(5, second.getDerivedPlanVersion().getVersionNumber());
        assertSame(second.getDerivedPlanVersion(), subscription.getPlanVersion());
    }

    // ---- Visa ---------------------------------------------------------------------------------

    @Test
    void approvalMustComeFromSomeoneElse() {
        PlanDerivation pending = service.create("SUB-2026-000001", spec("80000", null), "alice");
        when(derivationRepository.findByDerivationCode("DRV-2026-000001")).thenReturn(Optional.of(pending));

        assertThrows(ConflictException.class, () -> service.approve("DRV-2026-000001", "Alice"));
        assertEquals(PlanDerivation.Status.PENDING_APPROVAL, pending.getStatus());

        PlanDerivation approved = service.approve("DRV-2026-000001", "bob");

        assertEquals(PlanDerivation.Status.ACTIVE, approved.getStatus());
        assertEquals("bob", approved.getApprovedBy());
        assertEquals(PlanStatus.ACTIVE, approved.getDerivedPlanVersion().getStatus());
        assertSame(approved.getDerivedPlanVersion(), subscription.getPlanVersion());
        assertEquals(0, new BigDecimal("80000").compareTo(subscription.getTotalAmount()));
    }

    @Test
    void approvingSomethingAlreadyActiveIsRefused() {
        PlanDerivation active = service.create("SUB-2026-000001", spec("95000", null), "alice");
        when(derivationRepository.findByDerivationCode("DRV-2026-000001")).thenReturn(Optional.of(active));

        assertThrows(BadRequestException.class, () -> service.approve("DRV-2026-000001", "bob"));
    }

    @Test
    void rejectionArchivesTheDraftVersionAndKeepsTheSubscriptionOnCatalogue() {
        PlanDerivation pending = service.create("SUB-2026-000001", spec("80000", null), "alice");
        when(derivationRepository.findByDerivationCode("DRV-2026-000001")).thenReturn(Optional.of(pending));

        assertThrows(BadRequestException.class, () -> service.reject("DRV-2026-000001", " ", "bob"));
        PlanDerivation rejected = service.reject("DRV-2026-000001", "trop généreux", "bob");

        assertEquals(PlanDerivation.Status.REJECTED, rejected.getStatus());
        assertEquals("trop généreux", rejected.getRejectionReason());
        assertEquals(PlanStatus.ARCHIVED, rejected.getDerivedPlanVersion().getStatus());
        assertSame(catalogue, subscription.getPlanVersion());
    }

    // ---- Retour au catalogue ----------------------------------------------------------------

    @Test
    void revertBringsTheSubscriptionBackToTheActiveCatalogueVersion() {
        PlanDerivation active = service.create("SUB-2026-000001", spec("95000", null), "alice");
        when(derivationRepository.findFirstBySubscription_IdAndStatus(50L, PlanDerivation.Status.ACTIVE)).thenReturn(Optional.of(active));
        PlanVersion newerCatalogue = PlanVersion.builder().id(11L).plan(plan).versionNumber(4).name("Flex").status(PlanStatus.ACTIVE).build();
        when(planVersionRepository.findFirstByPlanAndStatusOrderByVersionNumberDesc(1L, "ACTIVE")).thenReturn(Optional.of(newerCatalogue));
        when(planPriceRepository.findAllByPlanVersion(11L)).thenReturn(List.of(price(newerCatalogue, "120000")));

        PlanDerivation ended = service.revert("SUB-2026-000001", "client parti");

        assertEquals(PlanDerivation.Status.EXPIRED, ended.getStatus());
        assertNotNull(ended.getEndedAt());
        assertTrue(ended.getReasonDetails().contains("client parti"));
        assertEquals(PlanStatus.ARCHIVED, ended.getDerivedPlanVersion().getStatus());
        assertSame(newerCatalogue, subscription.getPlanVersion(), "on revient au catalogue courant, pas à celui d'origine");
        assertEquals(0, new BigDecimal("120000").compareTo(subscription.getTotalAmount()));
    }

    @Test
    void revertWithoutActiveDerivationIsAConflict() {
        assertThrows(ConflictException.class, () -> service.revert("SUB-2026-000001", "rien"));
    }

    // ---- Renouvellement ---------------------------------------------------------------------

    @Test
    void keepBehaviourCountsPeriodsAndStays() {
        PlanDerivation active = service.create("SUB-2026-000001", spec("95000", null), "alice");
        when(derivationRepository.findFirstBySubscription_IdAndStatus(50L, PlanDerivation.Status.ACTIVE)).thenReturn(Optional.of(active));

        service.beforeRenewal(subscription);

        assertEquals(PlanDerivation.Status.ACTIVE, active.getStatus());
        assertEquals(1, active.getPeriodsApplied());
        assertSame(active.getDerivedPlanVersion(), subscription.getPlanVersion());
    }

    @Test
    void revertToCatalogueBehaviourEndsAtFirstRenewal() {
        PlanDerivationService.Spec spec = new PlanDerivationService.Spec(new BigDecimal("95000"), null, null, null, null, null, null,
                PlanDerivation.Reason.GOODWILL, null, null, null, PlanDerivation.RenewalBehaviour.REVERT_TO_CATALOGUE, null, null);
        PlanDerivation active = service.create("SUB-2026-000001", spec, "alice");
        when(derivationRepository.findFirstBySubscription_IdAndStatus(50L, PlanDerivation.Status.ACTIVE)).thenReturn(Optional.of(active));

        service.beforeRenewal(subscription);

        assertEquals(PlanDerivation.Status.EXPIRED, active.getStatus());
        assertSame(catalogue, subscription.getPlanVersion());
    }

    @Test
    void revertAfterPeriodsBehaviourEndsWhenTheCountIsReached() {
        PlanDerivationService.Spec spec = new PlanDerivationService.Spec(new BigDecimal("95000"), null, null, null, null, null, null,
                PlanDerivation.Reason.PILOT, null, null, null, PlanDerivation.RenewalBehaviour.REVERT_AFTER_PERIODS, 2, null);
        PlanDerivation active = service.create("SUB-2026-000001", spec, "alice");
        when(derivationRepository.findFirstBySubscription_IdAndStatus(50L, PlanDerivation.Status.ACTIVE)).thenReturn(Optional.of(active));

        service.beforeRenewal(subscription);
        assertEquals(PlanDerivation.Status.ACTIVE, active.getStatus());
        assertEquals(1, active.getPeriodsApplied());

        service.beforeRenewal(subscription);
        assertEquals(PlanDerivation.Status.EXPIRED, active.getStatus());
        assertSame(catalogue, subscription.getPlanVersion());
    }

    @Test
    void anExpiredEffectiveToRevertsWhateverTheBehaviour() {
        PlanDerivationService.Spec spec = new PlanDerivationService.Spec(new BigDecimal("95000"), null, null, null, null, null, null,
                PlanDerivation.Reason.GRANDFATHERING, null, LocalDate.now().minusMonths(6), LocalDate.now().minusDays(1),
                PlanDerivation.RenewalBehaviour.KEEP, null, null);
        PlanDerivation active = service.create("SUB-2026-000001", spec, "alice");
        when(derivationRepository.findFirstBySubscription_IdAndStatus(50L, PlanDerivation.Status.ACTIVE)).thenReturn(Optional.of(active));

        service.beforeRenewal(subscription);

        assertEquals(PlanDerivation.Status.EXPIRED, active.getStatus());
        assertTrue(active.getReasonDetails().contains("échéance"));
        assertSame(catalogue, subscription.getPlanVersion());
    }

    @Test
    void withoutDerivationRenewalDoesNothing() {
        service.beforeRenewal(subscription);

        verify(derivationRepository, never()).save(any());
        assertTrue(service.promotionsAllowed(subscription));
    }

    // ---- Politique ----------------------------------------------------------------------------

    @Test
    void floorPriceGoesOnACatalogueVersionOnly() {
        PlanVersion privateVersion = PlanVersion.builder().id(99L).plan(plan).versionNumber(9).scope(PlanVersion.Scope.SUBSCRIPTION).build();
        when(planVersionRepository.findById(99L)).thenReturn(Optional.of(privateVersion));

        assertThrows(BadRequestException.class, () -> service.setFloorPrice(99L, new BigDecimal("90000")));
        assertThrows(BadRequestException.class, () -> service.setFloorPrice(10L, new BigDecimal("-1")));

        assertEquals(0, new BigDecimal("90000").compareTo(service.setFloorPrice(10L, new BigDecimal("90000")).getFloorPrice()));
        assertNull(service.setFloorPrice(10L, null).getFloorPrice());
    }

    @Test
    void approvalRuleKeepsTheCeilingAboveTheVisaThreshold() {
        when(approvalRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(BadRequestException.class, () -> service.updateApprovalRule(new BigDecimal("30"), null, new BigDecimal("20")));
        assertThrows(BadRequestException.class, () -> service.updateApprovalRule(new BigDecimal("101"), null, null));

        PlanDerivationApprovalRule updated = service.updateApprovalRule(new BigDecimal("15"), new BigDecimal("-1"), new BigDecimal("60"));

        assertEquals(0, new BigDecimal("15").compareTo(updated.getMaxDiscountPercentWithoutApproval()));
        assertNull(updated.getMaxImpactWithoutApproval(), "un montant négatif retire la borne");
        assertEquals(0, new BigDecimal("60").compareTo(updated.getMaxDiscountPercentAllowed()));
    }

    // ---- Outils -------------------------------------------------------------------------------

    private static PlanDerivationService.Spec spec(String price, String details) {
        return new PlanDerivationService.Spec(new BigDecimal(price), null, null, null, null, null, null,
                PlanDerivation.Reason.NEGOTIATION, details, null, null, PlanDerivation.RenewalBehaviour.KEEP, null, null);
    }

    private static PlanPrice price(PlanVersion version, String amount) {
        return PlanPrice.builder().id(version.getId() * 10).planVersion(version).billingCycle(BillingCycle.MONTHLY).currency("XAF")
                .amount(new BigDecimal(amount)).setupFee(BigDecimal.ZERO).depositAmount(BigDecimal.ZERO).taxIncluded(true)
                .trialDays(0).commitmentMonths(0).build();
    }

    private Subscription subscriptionWithActive(PlanDerivation active) {
        when(derivationRepository.findFirstBySubscription_IdAndStatus(50L, PlanDerivation.Status.ACTIVE)).thenReturn(Optional.of(active));
        return subscription;
    }
}
