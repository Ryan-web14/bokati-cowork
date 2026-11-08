package com.sni.bokaticowork.features.subscription.subscription.service.support.subscription;

import com.sni.bokaticowork.features.billing.service.support.BillingTaxRuleResolver;
import com.sni.bokaticowork.features.subscription.promotion.pricing.service.SubscriptionPricingBridge;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreateSubscriptionRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.PlanPriceAmountCalculator;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionBillingSupport;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionCodeFactory;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionOwnerResolver;
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
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L'abonnement garde le prix de sa periode · frais d'entree et caution ne vont qu'a la premiere
 * facture. Les y laisser les faisait refacturer a chaque renouvellement, qui facture sous-total
 * et taxe de l'abonnement.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionCreationAmountsTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private SubscriptionOwnerResolver ownerResolver;
    @Mock private SubscriptionPlanResolver planResolver;
    @Mock private SubscriptionBillingSupport billingSupport;
    @Mock private SubscriptionCodeFactory codeFactory;
    @Mock private SubscriptionPricingBridge pricingBridge;
    @Mock private BillingTaxRuleResolver taxRuleResolver;
    @Mock private com.sni.bokaticowork.features.subscription.repository.SubscriptionItemRepository subscriptionItemRepository;
    @Mock private com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionPeriodCalculator periodCalculator;
    @Mock private SubscriptionLifecycleOperator lifecycleOperator;
    @Mock private com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter eventWriter;
    @Mock private com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository kycCaseRepository;
    @Mock private SubscriptionEmailNotifier emailNotifier;
    @Mock private com.sni.bokaticowork.features.portal.notification.service.MemberInAppNotifier memberInAppNotifier;
    @Mock private com.sni.bokaticowork.features.payment.service.interfaces.WalletService walletService;
    @Mock private com.sni.bokaticowork.features.payment.service.interfaces.WalletHoldService walletHoldService;
    @Mock private com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriberKycLevelGuard kycGuard;
    @Mock private com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionCommitmentService commitmentService;

    @InjectMocks
    private SubscriptionCreationOperator operator;

    @BeforeEach
    void setUp() {
        when(taxRuleResolver.defaultTaxProfile())
                .thenReturn(new BillingTaxRuleResolver.TaxProfile(new BigDecimal("18"), new BigDecimal("5")));
        ReflectionTestUtils.setField(operator, "priceCalculator", new PlanPriceAmountCalculator(taxRuleResolver));

        PlanVersion version = new PlanVersion();
        version.setId(7L);
        version.setName("Bureau ferme");
        PlanPrice price = new PlanPrice();
        price.setAmount(new BigDecimal("100000"));
        price.setSetupFee(new BigDecimal("20000"));
        price.setDepositAmount(new BigDecimal("50000"));
        price.setTaxIncluded(false);
        price.setBillingCycle(BillingCycle.MONTHLY);
        price.setCurrency("XAF");

        when(planResolver.resolvePlanVersion("PLAN", null)).thenReturn(version);
        when(planResolver.resolvePrice(version, BillingCycle.MONTHLY)).thenReturn(price);
        when(ownerResolver.resolve(SubscriberType.MEMBER, "MBR-1"))
                .thenReturn(new SubscriptionOwnerResolver.Owner("MBR-1", null, null, null));
        when(subscriptionRepository.findBySuscriberCodeAndPlanVersion("MBR-1", 7L)).thenReturn(List.of());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));
        when(codeFactory.nextSubscriptionNumber(any(), any(), any(), any())).thenReturn("SUB-1");
        when(pricingBridge.price(anyString(), anyString(), isNull(), any(), any(), any(), anyString(),
                any(), any(), anyList(), anyBoolean()))
                .thenReturn(new SubscriptionPricingBridge.PricedSubscription(new BigDecimal("100000"), new BigDecimal("20000"),
                        new BigDecimal("100000"), new BigDecimal("20000"), null));
    }

    @Test
    void shouldKeepOnlyThePeriodPriceOnTheSubscriptionAndBillTheRestOnTheFirstInvoice() {
        Subscription created = operator.create(new CreateSubscriptionRequest("PLAN", null, BillingCycle.MONTHLY,
                SubscriberType.MEMBER, "MBR-1", LocalDate.of(2026, 11, 1), true, null, false));

        // Periode : 100 000 HT, TVA 18 000, centime 900
        assertThat(created.getSubtotalAmount()).isEqualByComparingTo("100000");
        assertThat(created.getTaxAmount()).isEqualByComparingTo("18900");
        assertThat(created.getTotalAmount()).isEqualByComparingTo("118900");
        // Premiere facture inchangee : 120 000 HT + 22 680 de taxes + 50 000 de caution
        verify(billingSupport).createSubscriptionSetupItems(eq(created),
                argThat(v -> v.compareTo(new BigDecimal("100000")) == 0),
                argThat(v -> v.compareTo(new BigDecimal("20000")) == 0),
                argThat(v -> v.compareTo(new BigDecimal("50000")) == 0),
                argThat(v -> v.compareTo(new BigDecimal("192680")) == 0));
    }
}
