package com.sni.bokaticowork.features.subscription.promotion.pricing.service;

import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingEngine;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingResult;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.DiscountDocumentType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.DiscountSourceType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.RewardType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.TargetScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Le branchement du moteur sur la souscription.
 *
 * <p>Deux séparations y sont vérifiées, parce que les confondre se verrait sur une facture. La
 * remise s'applique <b>avant</b> la taxe, sans quoi on facturerait de la TVA sur une somme jamais
 * réclamée. Et le montant récurrent est réduit séparément des frais d'entrée, parce qu'ils ne
 * finissent pas au même endroit.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionPricingBridgeTest {

    @Mock private PricingEngine pricingEngine;
    @Mock private DiscountApplicationService discountApplicationService;

    @InjectMocks
    private SubscriptionPricingBridge bridge;

    @Test
    void leavesThePriceAloneWhenNothingApplies() {
        when(pricingEngine.evaluate(any())).thenReturn(result(List.of(), List.of()));

        SubscriptionPricingBridge.PricedSubscription priced = price("25000", "10000");

        assertEquals(new BigDecimal("25000"), priced.recurringAmount());
        assertEquals(new BigDecimal("10000"), priced.setupFee());
        assertTrue(priced.unchanged());
    }

    @Test
    void reducesTheRecurringAmountWithAGeneralDiscount() {
        when(pricingEngine.evaluate(any())).thenReturn(result(
                List.of(rule(RewardType.PERCENTAGE_OFF, "2500")), List.of()));

        SubscriptionPricingBridge.PricedSubscription priced = price("25000", "10000");

        assertEquals(new BigDecimal("22500"), priced.recurringAmount());
        // Les frais d'entree ne bougent pas : la remise ne les visait pas.
        assertEquals(new BigDecimal("10000"), priced.setupFee());
    }

    /**
     * La dispense de frais d'entrée ne doit toucher que les frais. La reporter sur le prix mensuel
     * ferait disparaître une faveur ponctuelle dans un montant récurrent.
     */
    @Test
    void waivesOnlyTheSetupFeeWhenThatIsWhatWasGranted() {
        when(pricingEngine.evaluate(any())).thenReturn(result(
                List.of(rule(RewardType.WAIVE_SETUP_FEE, "10000")), List.of()));

        SubscriptionPricingBridge.PricedSubscription priced = price("25000", "10000");

        assertEquals(new BigDecimal("25000"), priced.recurringAmount());
        assertEquals(0, priced.setupFee().signum());
    }

    /**
     * Le cas qui justifie qu'une promotion rende une règle par récompense : dix pour cent <i>et</i>
     * les frais offerts se séparent, chacun vers sa destination.
     */
    @Test
    void splitsAPromotionThatGrantsBothADiscountAndAWaiver() {
        when(pricingEngine.evaluate(any())).thenReturn(result(
                List.of(rule(RewardType.PERCENTAGE_OFF, "2500"), rule(RewardType.WAIVE_SETUP_FEE, "10000")),
                List.of()));

        SubscriptionPricingBridge.PricedSubscription priced = price("25000", "10000");

        assertEquals(new BigDecimal("22500"), priced.recurringAmount());
        assertEquals(0, priced.setupFee().signum());
    }

    /** Un tarif négocié remplace le prix : il arrive dans le montant, jamais comme une remise. */
    @Test
    void takesTheNegotiatedPriceFromTheGrid() {
        when(pricingEngine.evaluate(any())).thenReturn(result(
                List.of(),
                List.of(new PricingResult.PriceOverride("PLN-1", "GRT-PARTENAIRE", "Grille partenaire",
                        new BigDecimal("25000"), new BigDecimal("18000"), 1))));

        SubscriptionPricingBridge.PricedSubscription priced = price("25000", "10000");

        assertEquals(new BigDecimal("18000"), priced.recurringAmount());
        assertEquals(new BigDecimal("25000"), priced.catalogRecurringAmount());
    }

    @Test
    void appliesAPromotionOnTopOfTheNegotiatedPriceNotTheCatalogue() {
        when(pricingEngine.evaluate(any())).thenReturn(result(
                List.of(rule(RewardType.PERCENTAGE_OFF, "1800")),
                List.of(new PricingResult.PriceOverride("PLN-1", "GRT-PARTENAIRE", "Grille partenaire",
                        new BigDecimal("25000"), new BigDecimal("18000"), 1))));

        SubscriptionPricingBridge.PricedSubscription priced = price("25000", "10000");

        // Dix pour cent de dix-huit mille, retires du prix negocie.
        assertEquals(new BigDecimal("16200"), priced.recurringAmount());
    }

    @Test
    void neverProducesANegativeAmount() {
        when(pricingEngine.evaluate(any())).thenReturn(result(
                List.of(rule(RewardType.FIXED_AMOUNT_OFF, "99999")), List.of()));

        SubscriptionPricingBridge.PricedSubscription priced = price("25000", "10000");

        assertEquals(0, priced.recurringAmount().signum());
    }

    @Test
    void doesNotEvenCallTheEngineForAFreePlan() {
        SubscriptionPricingBridge.PricedSubscription priced = price("0", "0");

        assertEquals(0, priced.recurringAmount().signum());
        verifyNoInteractions(pricingEngine);
    }

    @Test
    void tracesTheDiscountsOnlyOnConfirmation() {
        when(pricingEngine.evaluate(any())).thenReturn(result(
                List.of(rule(RewardType.PERCENTAGE_OFF, "2500")), List.of()));
        SubscriptionPricingBridge.PricedSubscription priced = price("25000", "10000");
        verifyNoInteractions(discountApplicationService);

        bridge.confirm(priced, "MEMBER", "MEM-1", null, TargetScope.PLAN, "PLN-1", "MONTHLY", "XAF",
                List.of(), DiscountDocumentType.SUBSCRIPTION, "SUB-0001", "SYSTEM");

        verify(discountApplicationService).apply(any(), eq(DiscountDocumentType.SUBSCRIPTION),
                eq("SUB-0001"), anyString());
    }

    // -------------------------------------------------------------------------------------

    private SubscriptionPricingBridge.PricedSubscription price(String recurring, String setupFee) {
        return bridge.price("MEMBER", "MEM-1", null, TargetScope.PLAN, "PLN-1", "MONTHLY", "XAF",
                new BigDecimal(recurring), new BigDecimal(setupFee), List.of(), true);
    }

    private PricingResult.AppliedRule rule(RewardType type, String amount) {
        return new PricingResult.AppliedRule(1, DiscountSourceType.PROMOTION, "CAMPAGNE", "Campagne",
                type, null, new BigDecimal("35000"), new BigDecimal(amount), "Campagne");
    }

    private PricingResult result(List<PricingResult.AppliedRule> rules, List<PricingResult.PriceOverride> overrides) {
        return new PricingResult(new BigDecimal("35000"), new BigDecimal("35000"), BigDecimal.ZERO,
                new BigDecimal("35000"), "XAF", rules, List.of(), List.of(), overrides);
    }
}
