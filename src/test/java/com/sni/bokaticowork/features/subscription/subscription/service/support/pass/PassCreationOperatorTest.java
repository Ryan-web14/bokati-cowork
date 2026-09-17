package com.sni.bokaticowork.features.subscription.subscription.service.support.pass;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletHoldService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.subscription.repository.EntitlementDefinitionRepository;
import com.sni.bokaticowork.features.subscription.repository.PassEntitlementRepository;
import com.sni.bokaticowork.features.subscription.repository.PassPlanEntitlementRepository;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.repository.PassTransactionRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanVersionRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreatePassPurchaseRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassEventType;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlan;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import com.sni.bokaticowork.features.subscription.subscription.service.support.PlanPriceAmountCalculator;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriberKycLevelGuard;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionOwnerResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Vente d'un pass depuis un plan.
 *
 * <p>Quatre défauts sont figés ici, parce qu'ils étaient tous silencieux. Un pass gratuit était
 * activé sans qu'aucune trace ne le relate et sans que son bénéficiaire en soit averti. Le niveau
 * de vérification exigé par le plan était modélisé mais jamais lu. Le transfert et le partage
 * étaient forcés à faux quelle que soit la volonté du plan. Et un double envoi de la demande
 * achetait deux pass.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PassCreationOperatorTest {

    private static final String PLAN_CODE = "PPL-0001";

    @Mock private PassRepository passRepository;
    @Mock private PassEntitlementRepository passEntitlementRepository;
    @Mock private PassTransactionRepository passTransactionRepository;
    @Mock private EntitlementDefinitionRepository entitlementDefinitionRepository;
    @Mock private PassPlanEntitlementRepository planEntitlementRepository;
    @Mock private PlanVersionRepository planVersionRepository;
    @Mock private SequenceGeneratorFacade sequenceGenerator;
    @Mock private SubscriptionOwnerResolver ownerResolver;
    @Mock private SubscriptionService subscriptionService;
    @Mock private PassCodeFactory codeFactory;
    @Mock private PassPlanResolver planResolver;
    @Mock private PassPeriodCalculator periodCalculator;
    @Mock private PassBillingSupport billingSupport;
    @Mock private PassLifecycleOperator lifecycleOperator;
    @Mock private PassEventWriter eventWriter;
    @Mock private PassEmailNotifier emailNotifier;
    @Mock private PlanPriceAmountCalculator priceCalculator;
    @Mock private SubscriberKycLevelGuard kycGuard;
    @Mock private WalletService walletService;
    @Mock private WalletHoldService walletHoldService;

    @InjectMocks
    private PassCreationOperator operator;

    @Captor
    private ArgumentCaptor<Pass> passCaptor;

    @BeforeEach
    void setUp() {
        when(passRepository.save(any(Pass.class))).thenAnswer(call -> call.getArgument(0));
        when(codeFactory.nextPassNumber(any(), any())).thenReturn("PASS-0001");
        when(ownerResolver.resolve(any(), anyString())).thenReturn(
                new SubscriptionOwnerResolver.Owner("MEM-0001", null, null, null));
        when(periodCalculator.periodEnd(any(), any(), any())).thenReturn(Instant.parse("2026-12-31T23:59:00Z"));
        when(planEntitlementRepository.findAllByPassVersion(any())).thenReturn(List.of());
        when(lifecycleOperator.activate(any(), anyString(), anyString())).thenAnswer(call -> call.getArgument(0));
    }

    /**
     * Le défaut le plus grave, parce qu'il ne se voyait nulle part : la méthode sortait avant le
     * bloc qui écrit l'historique, l'événement et le courriel. Un pass offert était activé, et rien
     * ne le relatait.
     */
    @Test
    void writesTheCreationTraceEvenWhenThePassIsFree() {
        givenPlan(BigDecimal.ZERO);

        operator.createFromPlan(PLAN_CODE, purchase(null));

        verify(eventWriter).writeHistory(any(Pass.class), isNull(), eq(PassStatus.PENDING_ACTIVATION), anyString(), anyString());
        verify(eventWriter).writeEvent(any(Pass.class), eq(PassEventType.PASS_CREATED), isNull());
        verify(emailNotifier).notifyCreated(any(Pass.class));
        verify(lifecycleOperator).activate(any(Pass.class), anyString(), anyString());
        verify(billingSupport, never()).createAndInvoice(any());
    }

    @Test
    void writesTheSameTraceWhenThePassIsPaidAndLeavesItAwaitingActivation() {
        givenPlan(new BigDecimal("25000"));

        operator.createFromPlan(PLAN_CODE, purchase(null));

        verify(eventWriter).writeEvent(any(Pass.class), eq(PassEventType.PASS_CREATED), isNull());
        verify(emailNotifier).notifyCreated(any(Pass.class));
        verify(billingSupport).createAndInvoice(any(Pass.class));
        // L'activation appartient au paiement, pas a la creation.
        verify(lifecycleOperator, never()).activate(any(), anyString(), anyString());
    }

    /**
     * Le statut ACTIVE ne se pose qu'à l'activation, qui est aussi ce qui accorde les droits,
     * planifie le renouvellement et demande le contrat. Le poser à la main court-circuitait tout.
     */
    @Test
    void neverPersistsAnActiveStatusItself() {
        givenPlan(BigDecimal.ZERO);

        operator.createFromPlan(PLAN_CODE, purchase(null));

        verify(passRepository).save(passCaptor.capture());
        assertEquals(PassStatus.PENDING_ACTIVATION, passCaptor.getValue().getStatus());
    }

    @Test
    void readsTransferAndSharingFromThePlanInsteadOfForcingThemToFalse() {
        PassPlanVersion version = givenPlan(BigDecimal.ZERO);
        version.setTransferable(Boolean.TRUE);
        version.setShareable(Boolean.TRUE);

        operator.createFromPlan(PLAN_CODE, purchase(null));

        verify(passRepository).save(passCaptor.capture());
        assertTrue(passCaptor.getValue().getTransferable());
        assertTrue(passCaptor.getValue().getShareable());
    }

    @Test
    void keepsTheDefaultWhenThePlanSaysNothing() {
        givenPlan(BigDecimal.ZERO);

        operator.createFromPlan(PLAN_CODE, purchase(null));

        verify(passRepository).save(passCaptor.capture());
        assertFalse(passCaptor.getValue().getTransferable());
        assertFalse(passCaptor.getValue().getShareable());
    }

    @Test
    void enforcesTheKycLevelDemandedByThePlan() {
        PassPlanVersion version = givenPlan(BigDecimal.ZERO);
        version.setRequiredKycLevel(3);
        org.mockito.Mockito.doThrow(new BadRequestException("KYC level 3 required for this pass plan"))
                .when(kycGuard).require(eq(3), any(), anyString());

        assertThrows(BadRequestException.class, () -> operator.createFromPlan(PLAN_CODE, purchase(null)));

        verify(passRepository, never()).save(any());
    }

    /**
     * Sans cela, un double envoi produisait deux pass et deux factures, que le titulaire ne
     * découvrait qu'en les recevant.
     */
    @Test
    void returnsTheExistingPassWhenTheSameRequestIsSentTwice() {
        Pass alreadyCreated = Pass.builder().passNumber("PASS-0001").idempotencyKey("KEY-1").build();
        when(passRepository.findByIdempotencyKey("KEY-1")).thenReturn(Optional.of(alreadyCreated));

        Pass result = operator.createFromPlan(PLAN_CODE, purchase("KEY-1"));

        assertSame(alreadyCreated, result);
        verify(passRepository, never()).save(any());
        verifyNoInteractions(planResolver, billingSupport, eventWriter, emailNotifier);
    }

    // -------------------------------------------------------------------------------------

    private PassPlanVersion givenPlan(BigDecimal amount) {
        PassPlan plan = new PassPlan();
        plan.setPassType(PassType.DAY_PASS);

        PassPlanVersion version = new PassPlanVersion();
        version.setPlan(plan);
        version.setName("Pass journée");
        version.setRequiredKycLevel(1);
        version.setTransferable(Boolean.FALSE);
        version.setShareable(Boolean.FALSE);

        PassPlanPrice price = new PassPlanPrice();
        price.setCurrency("XAF");
        price.setAmount(amount);

        when(planResolver.resolveVersion(eq(PLAN_CODE), any())).thenReturn(version);
        when(planResolver.resolvePrice(eq(version), any())).thenReturn(price);
        when(priceCalculator.compute(any(), any(), any(), any())).thenReturn(
                new PlanPriceAmountCalculator.Amounts(amount, BigDecimal.ZERO, amount));
        return version;
    }

    private CreatePassPurchaseRequest purchase(String idempotencyKey) {
        return new CreatePassPurchaseRequest(
                SubscriberType.MEMBER, "MEM-0001", null, "XAF", null, false, null, idempotencyKey);
    }
}
