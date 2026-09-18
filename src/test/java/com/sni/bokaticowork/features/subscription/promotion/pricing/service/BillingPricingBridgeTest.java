package com.sni.bokaticowork.features.subscription.promotion.pricing.service;

import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentDiscountRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentLineRequest;
import com.sni.bokaticowork.features.billing.enums.BillingDiscountType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingContext;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingEngine;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingResult;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.DiscountDocumentType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.DiscountSourceType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.RewardType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.TargetScope;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Le branchement du moteur sur la facturation.
 *
 * <p>Deux conventions s'y rencontrent, et les confondre produirait une facture fausse. Une
 * <b>promotion</b> réduit un montant et doit donc apparaître comme une remise, avec son origine.
 * Une <b>grille tarifaire</b> remplace un prix et doit donc arriver dans le prix unitaire de la
 * ligne, jamais dans une ligne de remise qui laisserait croire à une faveur ponctuelle.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingPricingBridgeTest {

    @Mock private PricingEngine pricingEngine;
    @Mock private DiscountApplicationService discountApplicationService;

    @InjectMocks
    private BillingPricingBridge bridge;

    @Captor
    private ArgumentCaptor<PricingContext> contextCaptor;

    @Test
    void turnsAnAppliedPromotionIntoADiscountCarryingItsOrigin() {
        when(pricingEngine.evaluate(any())).thenReturn(result(
                List.of(new PricingResult.AppliedRule(1, DiscountSourceType.PROMOTION, "RENTREE",
                        "Offre de rentrée", RewardType.PERCENTAGE_OFF, null,
                        new BigDecimal("10000"), new BigDecimal("1000"), "Offre de rentrée")),
                List.of()));

        BillingPricingBridge.PricedDocument priced = bridge.price(
                "MEMBER", "MEM-1", null, "XAF", "MONTHLY", List.of(line("PLN-1", "10000")), List.of(), true);

        assertEquals(1, priced.discounts().size());
        CreateBillingDocumentDiscountRequest discount = priced.discounts().getFirst();
        assertEquals("PROMOTION", discount.sourceType());
        assertEquals("RENTREE", discount.sourceCode());
        assertEquals("Offre de rentrée", discount.description());
        // Le moteur a deja calcule : la facture recoit un montant, pas un taux a recalculer.
        assertEquals(BillingDiscountType.FIXED_AMOUNT, discount.discountType());
        assertEquals(new BigDecimal("1000"), discount.value());
    }

    /**
     * La distinction qui justifie tout ce branchement : un tarif négocié n'est pas une remise. Il
     * descend dans le prix de la ligne et ne produit aucune ligne de remise.
     */
    @Test
    void putsAGridPriceIntoTheLineAndNeverIntoADiscount() {
        when(pricingEngine.evaluate(any())).thenReturn(result(
                List.of(),
                List.of(new PricingResult.PriceOverride("PLN-1", "GRT-PARTENAIRE", "Grille partenaire",
                        new BigDecimal("10000"), new BigDecimal("7000"), 1))));

        BillingPricingBridge.PricedDocument priced = bridge.price(
                "MEMBER", "MEM-1", null, "XAF", "MONTHLY", List.of(line("PLN-1", "10000")), List.of(), true);

        assertTrue(priced.discounts().isEmpty());
        assertEquals(new BigDecimal("7000"), priced.lines().getFirst().unitPrice());
        // Le reste de la ligne est intact : seule la valeur negociee change.
        assertEquals("PLN-1", priced.lines().getFirst().itemCode());
        assertEquals("Plan PLN-1", priced.lines().getFirst().description());
    }

    @Test
    void leavesTheLinesUntouchedWhenNoGridApplies() {
        List<CreateBillingDocumentLineRequest> lines = List.of(line("PLN-1", "10000"));
        when(pricingEngine.evaluate(any())).thenReturn(result(List.of(), List.of()));

        BillingPricingBridge.PricedDocument priced = bridge.price(
                "MEMBER", "MEM-1", null, "XAF", "MONTHLY", lines, List.of(), true);

        assertSame(lines, priced.lines());
    }

    /**
     * Le type de source d'une ligne de facture devient la portée du moteur. Un type que le moteur
     * ne connaît pas n'est pas une erreur : la facturation en connaît que lui n'a pas à connaître,
     * et la ligne reste tarifable pour elle-même.
     */
    @Test
    void mapsTheBillingSourceTypeToAPricingScope() {
        when(pricingEngine.evaluate(any())).thenReturn(result(List.of(), List.of()));

        bridge.price("MEMBER", "MEM-1", null, "XAF", "MONTHLY", List.of(
                lineWithSource("PLN-1", "PLAN", "PLN-1"),
                lineWithSource("DIVERS", "QUELQUE_CHOSE_DINCONNU", null)), List.of(), true);

        verify(pricingEngine).evaluate(contextCaptor.capture());
        assertEquals(TargetScope.PLAN, contextCaptor.getValue().lines().get(0).scope());
        assertEquals(TargetScope.LINE, contextCaptor.getValue().lines().get(1).scope());
    }

    /**
     * Tarifer n'engage rien. C'est ce qui permet de recalculer un brouillon autant de fois qu'on
     * veut sans épuiser une campagne.
     */
    @Test
    void doesNotConsumeAnythingWhenMerelyPricing() {
        when(pricingEngine.evaluate(any())).thenReturn(result(
                List.of(new PricingResult.AppliedRule(1, DiscountSourceType.PROMOTION, "RENTREE",
                        "Offre", RewardType.PERCENTAGE_OFF, null,
                        new BigDecimal("10000"), new BigDecimal("1000"), "Offre")),
                List.of()));

        bridge.price("MEMBER", "MEM-1", null, "XAF", "MONTHLY", List.of(line("PLN-1", "10000")), List.of(), true);

        verifyNoInteractions(discountApplicationService);
    }

    @Test
    void tracesTheDiscountsOnlyWhenTheDocumentIsConfirmed() {
        PricingResult result = result(
                List.of(new PricingResult.AppliedRule(1, DiscountSourceType.PROMOTION, "RENTREE",
                        "Offre", RewardType.PERCENTAGE_OFF, null,
                        new BigDecimal("10000"), new BigDecimal("1000"), "Offre")),
                List.of());
        when(pricingEngine.evaluate(any())).thenReturn(result);
        BillingPricingBridge.PricedDocument priced = bridge.price(
                "MEMBER", "MEM-1", null, "XAF", "MONTHLY", List.of(line("PLN-1", "10000")), List.of(), true);

        bridge.confirm(priced, "MEMBER", "MEM-1", null, "XAF", "MONTHLY", List.of(), "INV-0001", "SYSTEM");

        verify(discountApplicationService).apply(any(), eq(DiscountDocumentType.BILLING_DOCUMENT),
                eq("INV-0001"), anyString());
    }

    @Test
    void confirmsNothingWhenNoRuleApplied() {
        when(pricingEngine.evaluate(any())).thenReturn(result(List.of(), List.of()));
        BillingPricingBridge.PricedDocument priced = bridge.price(
                "MEMBER", "MEM-1", null, "XAF", "MONTHLY", List.of(line("PLN-1", "10000")), List.of(), true);

        bridge.confirm(priced, "MEMBER", "MEM-1", null, "XAF", "MONTHLY", List.of(), "INV-0001", "SYSTEM");

        verifyNoInteractions(discountApplicationService);
    }

    // -------------------------------------------------------------------------------------

    private PricingResult result(List<PricingResult.AppliedRule> rules, List<PricingResult.PriceOverride> overrides) {
        return new PricingResult(new BigDecimal("10000"), new BigDecimal("10000"), BigDecimal.ZERO,
                new BigDecimal("10000"), "XAF", rules, List.of(), List.of(), overrides);
    }

    private CreateBillingDocumentLineRequest line(String itemCode, String unitPrice) {
        return lineWithSource(itemCode, "PLAN", itemCode);
    }

    private CreateBillingDocumentLineRequest lineWithSource(String itemCode, String sourceType, String sourceCode) {
        return new CreateBillingDocumentLineRequest(
                1, null, itemCode, "Plan " + itemCode, null, BigDecimal.ONE, new BigDecimal("10000"),
                null, null, true, false, null, null, sourceType, sourceCode, null,
                itemCode, null, false, null);
    }
}
