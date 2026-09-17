package com.sni.bokaticowork.features.subscription.promotion.pricing.engine;

import com.sni.bokaticowork.features.subscription.promotion.enums.DiscountType;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.ConditionOperator;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.ConditionType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.DiscountSourceType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.PromotionType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.RewardType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.TargetScope;
import com.sni.bokaticowork.features.subscription.promotion.pricing.model.PromotionCondition;
import com.sni.bokaticowork.features.subscription.promotion.pricing.model.PromotionReward;
import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.service.PromotionAudienceFilter;
import com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.service.PriceListResolver;
import com.sni.bokaticowork.features.subscription.promotion.pricing.repository.PromotionConditionRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.repository.PromotionRewardRepository;
import com.sni.bokaticowork.features.subscription.promotion.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Le moteur de tarification.
 *
 * <p>Jusqu'ici aucun montant de remise n'était jamais calculé nulle part : on pouvait créer une
 * promotion, l'activer, enregistrer qu'un abonné l'avait utilisée, et rien ne se passait. Ces tests
 * fixent l'ordre d'évaluation et les garde-fous, qui sont les deux choses dont un écart devient
 * inexplicable trois mois plus tard.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PricingEngineTest {

    @Mock private PromotionRepository promotionRepository;
    @Mock private PromotionConditionRepository conditionRepository;
    @Mock private PromotionRewardRepository rewardRepository;
    @Mock private CouponLookup couponLookup;
    @Mock private PriceListResolver priceListResolver;
    @Mock private PromotionAudienceFilter audienceFilter;

    private PricingEngine engine;

    private final AtomicLong ids = new AtomicLong(1);
    private final List<Promotion> promotions = new ArrayList<>();
    private final List<PromotionCondition> conditions = new ArrayList<>();
    private final List<PromotionReward> rewards = new ArrayList<>();

    @BeforeEach
    void setUp() {
        engine = new PricingEngine(promotionRepository, conditionRepository, rewardRepository,
                new PromotionConditionEvaluator(), couponLookup, priceListResolver, audienceFilter);
        when(promotionRepository.findEvaluable(anyString(), any(Instant.class))).thenReturn(promotions);
        when(conditionRepository.findAllByPromotionIds(any())).thenReturn(conditions);
        when(rewardRepository.findAllByPromotionIds(any())).thenReturn(rewards);
        when(couponLookup.resolve(any(), any(), any(), any())).thenReturn(List.of());
        // Toutes visibles par defaut : le ciblage a ses propres tests.
        when(audienceFilter.visibleTo(any(), any())).thenAnswer(call -> {
            java.util.Collection<Long> given = call.getArgument(0);
            return given == null ? java.util.Set.of() : new java.util.HashSet<>(given);
        });
        // Aucune grille par defaut : ces tests portent sur les campagnes, pas sur la tarification.
        when(priceListResolver.resolve(any())).thenAnswer(call -> {
            // Null-safe : re-stuber cette methode dans un test la rappelle avec un argument nul.
            PricingContext given = call.getArgument(0);
            return new PriceListResolver.Resolution(given == null ? List.of() : given.linesOrEmpty(), List.of());
        });
    }

    @Test
    void appliesAPercentageOnTheWholeOrder() {
        promotion("PROMO10", 100, reward(RewardType.PERCENTAGE_OFF, "10", TargetScope.WHOLE_ORDER, null));

        PricingResult result = engine.evaluate(context(line("L1", TargetScope.PLAN, "PLN-1", 1, "10000")));

        assertEquals(new BigDecimal("10000"), result.originalTotal());
        assertEquals(new BigDecimal("1000"), result.discountTotal());
        assertEquals(new BigDecimal("9000"), result.finalTotal());
        assertEquals(1, result.appliedRules().size());
    }

    /**
     * Le plafond absolu existe précisément pour ce cas : quatre-vingts pour cent sur un abonnement
     * annuel d'entreprise n'est presque jamais l'intention de celui qui a saisi la campagne.
     */
    @Test
    void capsAPercentageWithTheAbsoluteCeiling() {
        Promotion promotion = promotion("PROMO80", 100, reward(RewardType.PERCENTAGE_OFF, "80", TargetScope.WHOLE_ORDER, null));
        promotion.setMaxDiscountAmount(new BigDecimal("5000"));

        PricingResult result = engine.evaluate(context(line("L1", TargetScope.PLAN, "PLN-1", 1, "100000")));

        assertEquals(new BigDecimal("5000"), result.discountTotal());
    }

    @Test
    void neverLetsTheTotalFallBelowZero() {
        promotion("PROMOBIG", 100, reward(RewardType.FIXED_AMOUNT_OFF, "999999", TargetScope.WHOLE_ORDER, null));

        PricingResult result = engine.evaluate(context(line("L1", TargetScope.PLAN, "PLN-1", 1, "10000")));

        assertEquals(new BigDecimal("10000"), result.discountTotal());
        assertEquals(0, result.finalTotal().signum());
    }

    @Test
    void stopsWhenTheCampaignBudgetIsExhausted() {
        Promotion promotion = promotion("PROMOBUDGET", 100, reward(RewardType.PERCENTAGE_OFF, "50", TargetScope.WHOLE_ORDER, null));
        promotion.setBudgetAmount(new BigDecimal("3000"));
        promotion.setConsumedBudgetAmount(new BigDecimal("2000"));

        PricingResult result = engine.evaluate(context(line("L1", TargetScope.PLAN, "PLN-1", 1, "10000")));

        // Cinq mille auraient ete accordes, mille restent au budget.
        assertEquals(new BigDecimal("1000"), result.discountTotal());
    }

    @Test
    void refusesOutrightWhenNothingRemainsInTheBudget() {
        Promotion promotion = promotion("PROMODONE", 100, reward(RewardType.PERCENTAGE_OFF, "50", TargetScope.WHOLE_ORDER, null));
        promotion.setBudgetAmount(new BigDecimal("2000"));
        promotion.setConsumedBudgetAmount(new BigDecimal("2000"));

        PricingResult result = engine.evaluate(context(line("L1", TargetScope.PLAN, "PLN-1", 1, "10000")));

        assertEquals(0, result.discountTotal().signum());
        assertTrue(result.rejections().getFirst().reason().contains("budget"));
    }

    /**
     * L'ordre change le montant final, donc la priorité doit décider, pas l'ordre d'insertion.
     */
    @Test
    void evaluatesInPriorityOrderAndStopsOnAnExclusivePromotion() {
        promotion("PREMIERE", 10, reward(RewardType.FIXED_AMOUNT_OFF, "1000", TargetScope.WHOLE_ORDER, null))
                .setExclusive(Boolean.TRUE);
        promotion("SECONDE", 20, reward(RewardType.FIXED_AMOUNT_OFF, "5000", TargetScope.WHOLE_ORDER, null));

        PricingResult result = engine.evaluate(context(line("L1", TargetScope.PLAN, "PLN-1", 1, "10000")));

        assertEquals(1, result.appliedRules().size());
        assertEquals("PREMIERE", result.appliedRules().getFirst().sourceCode());
        assertEquals(new BigDecimal("1000"), result.discountTotal());
    }

    @Test
    void refusesToStackANonStackablePromotionOnTopOfAnother() {
        promotion("CUMULABLE", 10, reward(RewardType.FIXED_AMOUNT_OFF, "1000", TargetScope.WHOLE_ORDER, null));
        promotion("SEULE", 20, reward(RewardType.FIXED_AMOUNT_OFF, "2000", TargetScope.WHOLE_ORDER, null))
                .setStackable(Boolean.FALSE);

        PricingResult result = engine.evaluate(context(line("L1", TargetScope.PLAN, "PLN-1", 1, "10000")));

        assertEquals(1, result.appliedRules().size());
        assertEquals(new BigDecimal("1000"), result.discountTotal());
        assertTrue(result.rejections().getFirst().reason().contains("cumule"));
    }

    /**
     * Une remise ciblée ne doit pas se calculer sur ce qu'elle ne vise pas : dix pour cent sur les
     * salles ne porte pas sur les cafés.
     */
    @Test
    void computesATargetedRewardOnItsOwnLinesOnly() {
        promotion("SALLES", 100, reward(RewardType.PERCENTAGE_OFF, "10", TargetScope.RESOURCE, "SALLE-A"));

        PricingResult result = engine.evaluate(context(
                line("L1", TargetScope.RESOURCE, "SALLE-A", 1, "10000"),
                line("L2", TargetScope.INVENTORY_ITEM, "CAFE", 4, "500")));

        assertEquals(new BigDecimal("12000"), result.originalTotal());
        assertEquals(new BigDecimal("1000"), result.discountTotal());
    }

    @Test
    void ignoresAPromotionWhoseConditionsAreNotMet() {
        Promotion promotion = promotion("NOUVEAUX", 100, reward(RewardType.PERCENTAGE_OFF, "10", TargetScope.WHOLE_ORDER, null));
        condition(promotion, ConditionType.FIRST_PURCHASE, ConditionOperator.EQUALS, "true", null);

        PricingResult notFirst = engine.evaluate(context(false, line("L1", TargetScope.PLAN, "PLN-1", 1, "10000")));
        assertEquals(0, notFirst.discountTotal().signum());

        PricingResult first = engine.evaluate(context(true, line("L1", TargetScope.PLAN, "PLN-1", 1, "10000")));
        assertEquals(new BigDecimal("1000"), first.discountTotal());
    }

    @Test
    void appliesAMinimumAmountConditionInclusively() {
        Promotion promotion = promotion("GROSPANIER", 100, reward(RewardType.FIXED_AMOUNT_OFF, "500", TargetScope.WHOLE_ORDER, null));
        condition(promotion, ConditionType.MIN_AMOUNT, ConditionOperator.GREATER_THAN, "10000", null);

        // Un seuil commercial se pense en « a partir de » : dix mille pile doit passer.
        assertEquals(new BigDecimal("500"),
                engine.evaluate(context(line("L1", TargetScope.PLAN, "PLN-1", 1, "10000"))).discountTotal());
        assertEquals(0,
                engine.evaluate(context(line("L1", TargetScope.PLAN, "PLN-1", 1, "9999"))).discountTotal().signum());
    }

    /**
     * Un prix déjà négocié n'a pas vocation à recevoir une remise supplémentaire. L'évaluation
     * s'arrête alors avant même de regarder les campagnes.
     */
    @Test
    void leavesANegotiatedPriceUntouched() {
        promotion("PROMO10", 100, reward(RewardType.PERCENTAGE_OFF, "10", TargetScope.WHOLE_ORDER, null));

        PricingContext negotiated = new PricingContext(
                "MEMBER", "MEM-1", null, null, null, false,
                List.of(line("L1", TargetScope.PLAN, "PLN-1", 1, "10000")),
                null, null, null, null, "XAF", List.of(), Instant.now(), false);

        PricingResult result = engine.evaluate(negotiated);

        assertEquals(0, result.discountTotal().signum());
        assertEquals(new BigDecimal("10000"), result.finalTotal());
    }

    /**
     * Une récompense non monétaire ne se soustrait pas d'une ligne. La confondre avec une remise
     * produirait une facture fausse.
     */
    @Test
    void reportsANonMonetaryRewardAsAGrantRatherThanADiscount() {
        promotion("ESSAI", 100, reward(RewardType.FREE_TRIAL_DAYS, "30", TargetScope.WHOLE_ORDER, null));

        PricingResult result = engine.evaluate(context(line("L1", TargetScope.PLAN, "PLN-1", 1, "10000")));

        assertEquals(0, result.discountTotal().signum());
        assertEquals(1, result.grants().size());
        assertEquals(RewardType.FREE_TRIAL_DAYS, result.grants().getFirst().rewardType());
    }

    @Test
    void waivesTheSetupFeeWithoutTouchingTheRecurringPrice() {
        promotion("SANSFRAIS", 100, reward(RewardType.WAIVE_SETUP_FEE, null, TargetScope.WHOLE_ORDER, null));

        PricingContext context = new PricingContext(
                "MEMBER", "MEM-1", null, null, null, false,
                List.of(new PricingContext.PricingLine("L1", TargetScope.PLAN, "PLN-1", null, "Plan",
                        1, new BigDecimal("10000"), new BigDecimal("2500"))),
                null, null, null, null, "XAF", List.of(), Instant.now(), true);

        PricingResult result = engine.evaluate(context);

        assertEquals(new BigDecimal("12500"), result.originalTotal());
        assertEquals(new BigDecimal("2500"), result.discountTotal());
        assertEquals(new BigDecimal("10000"), result.finalTotal());
    }

    /**
     * La règle qui protège tout le ciblage : une promotion nominative n'existe pas pour qui n'en
     * est pas bénéficiaire. Elle ne doit apparaître ni dans son panier, ni dans ses refus, où sa
     * seule mention apprendrait qu'elle existe.
     */
    @Test
    void doesNotApplyOrEvenMentionAPromotionTheSubscriberCannotSee() {
        promotion("GESTE-COMMERCIAL", 100, reward(RewardType.PERCENTAGE_OFF, "30", TargetScope.WHOLE_ORDER, null));
        when(audienceFilter.visibleTo(any(), any())).thenReturn(java.util.Set.of());

        PricingResult result = engine.evaluate(context(line("L1", TargetScope.PLAN, "PLN-1", 1, "10000")));

        assertEquals(0, result.discountTotal().signum());
        assertTrue(result.appliedRules().isEmpty());
        assertTrue(result.rejections().isEmpty());
    }

    // -------------------------------------------------------------------------------------
    // Étape 2 · grille tarifaire
    // -------------------------------------------------------------------------------------

    /**
     * L'ordre compte : une remise de dix pour cent accordée à un partenaire porte sur son tarif
     * négocié, pas sur un tarif public qu'il ne paie jamais. Inverser les deux étapes lui offrirait
     * une réduction calculée sur un prix qui n'est pas le sien.
     */
    @Test
    void appliesPromotionsOnTheNegotiatedPriceNotOnTheCatalogue() {
        promotion("PROMO10", 100, reward(RewardType.PERCENTAGE_OFF, "10", TargetScope.WHOLE_ORDER, null));
        PricingContext.PricingLine catalogue = line("L1", TargetScope.PLAN, "PLN-1", 1, "10000");
        PricingContext.PricingLine negotiated = line("L1", TargetScope.PLAN, "PLN-1", 1, "7000");
        when(priceListResolver.resolve(any())).thenReturn(new PriceListResolver.Resolution(
                List.of(negotiated),
                List.of(new PriceListResolver.PriceOverride("L1", "GRT-PARTENAIRE", "Grille partenaire",
                        new BigDecimal("10000"), new BigDecimal("7000"), 1))));

        PricingResult result = engine.evaluate(context(catalogue));

        assertEquals(new BigDecimal("10000"), result.catalogTotal());
        assertEquals(new BigDecimal("7000"), result.originalTotal());
        // Dix pour cent de sept mille, non de dix mille.
        assertEquals(new BigDecimal("700"), result.discountTotal());
        assertEquals(new BigDecimal("6300"), result.finalTotal());
    }

    /** Une grille n'est pas une remise : elle ne figure jamais dans les règles appliquées. */
    @Test
    void neverReportsAGridAsADiscount() {
        PricingContext.PricingLine negotiated = line("L1", TargetScope.PLAN, "PLN-1", 1, "7000");
        when(priceListResolver.resolve(any())).thenReturn(new PriceListResolver.Resolution(
                List.of(negotiated),
                List.of(new PriceListResolver.PriceOverride("L1", "GRT-PARTENAIRE", "Grille partenaire",
                        new BigDecimal("10000"), new BigDecimal("7000"), 1))));

        PricingResult result = engine.evaluate(context(line("L1", TargetScope.PLAN, "PLN-1", 1, "10000")));

        assertTrue(result.appliedRules().isEmpty());
        assertEquals(0, result.discountTotal().signum());
        assertEquals(1, result.priceOverrides().size());
        assertEquals("GRT-PARTENAIRE", result.priceOverrides().getFirst().priceListCode());
    }

    // -------------------------------------------------------------------------------------
    // Étape 5 · coupons saisis
    // -------------------------------------------------------------------------------------

    @Test
    void appliesACouponAfterTheAutomaticPromotions() {
        Promotion automatic = promotion("AUTO", 10, reward(RewardType.FIXED_AMOUNT_OFF, "1000", TargetScope.WHOLE_ORDER, null));
        Promotion coupon = promotion("CAMPAGNE-CODE", 50, reward(RewardType.PERCENTAGE_OFF, "10", TargetScope.WHOLE_ORDER, null));
        promotions.remove(coupon);
        when(couponLookup.resolve(any(), any(), any(), any()))
                .thenReturn(List.of(new CouponLookup.ResolvedCoupon("BIENVENUE", coupon, null)));

        PricingResult result = engine.evaluate(contextWithCoupon("BIENVENUE",
                line("L1", TargetScope.PLAN, "PLN-1", 1, "10000")));

        assertEquals(2, result.appliedRules().size());
        assertEquals("AUTO", result.appliedRules().getFirst().sourceCode());
        assertEquals("BIENVENUE", result.appliedRules().get(1).sourceCode());
        // Les remises ne se composent pas : les dix pour cent portent sur le prix d'origine, pas
        // sur les neuf mille restants. Mille plus mille, et non mille plus neuf cents.
        assertEquals(new BigDecimal("2000"), result.discountTotal());
        assertEquals("AUTO", automatic.getCode());
    }

    /**
     * Un panier à trois codes dont un périmé doit passer, pas échouer en bloc. Et le motif du refus
     * doit remonter tel quel : un code refusé sans explication renvoie le client vers le support.
     */
    @Test
    void reportsARefusedCouponWithoutBlockingTheOthers() {
        Promotion valid = promotion("CAMPAGNE-OK", 50, reward(RewardType.FIXED_AMOUNT_OFF, "500", TargetScope.WHOLE_ORDER, null));
        promotions.remove(valid);
        when(couponLookup.resolve(any(), any(), any(), any())).thenReturn(List.of(
                new CouponLookup.ResolvedCoupon("PERIME", null, "Ce code a expiré"),
                new CouponLookup.ResolvedCoupon("VALIDE", valid, null)));

        PricingResult result = engine.evaluate(contextWithCoupon("PERIME",
                line("L1", TargetScope.PLAN, "PLN-1", 1, "10000")));

        assertEquals(new BigDecimal("500"), result.discountTotal());
        assertEquals(1, result.rejections().size());
        assertEquals("Ce code a expiré", result.rejections().getFirst().reason());
        assertEquals(DiscountSourceType.COUPON, result.rejections().getFirst().sourceType());
    }

    @Test
    void doesNotLookUpCouponsWhenThePriceIsAlreadyNegotiated() {
        PricingContext negotiated = new PricingContext(
                "MEMBER", "MEM-1", null, null, null, false,
                List.of(line("L1", TargetScope.PLAN, "PLN-1", 1, "10000")),
                null, null, null, null, "XAF", List.of("BIENVENUE"), Instant.now(), false);

        PricingResult result = engine.evaluate(negotiated);

        assertEquals(0, result.discountTotal().signum());
        verifyNoInteractions(couponLookup);
    }

    private PricingContext contextWithCoupon(String code, PricingContext.PricingLine... lines) {
        return new PricingContext(
                "MEMBER", "MEM-1", null, null, null, false,
                List.of(lines), null, null, null, null, "XAF", List.of(code), Instant.now(), true);
    }

    // -------------------------------------------------------------------------------------

    private Promotion promotion(String code, int priority, PromotionReward... promotionRewards) {
        Promotion promotion = Promotion.builder()
                .code(code)
                .name("Campagne " + code)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.ZERO)
                .promotionType(PromotionType.AUTOMATIC)
                .priority(priority)
                .stackable(Boolean.TRUE)
                .exclusive(Boolean.FALSE)
                .consumedBudgetAmount(BigDecimal.ZERO)
                .totalDiscountGranted(BigDecimal.ZERO)
                .redemptionCount(0)
                .startsAt(Instant.now().minusSeconds(3600))
                .build();
        promotion.setId(ids.getAndIncrement());
        promotions.add(promotion);
        promotions.sort((left, right) -> Integer.compare(left.getPriority(), right.getPriority()));
        for (PromotionReward reward : promotionRewards) {
            reward.setPromotion(promotion);
            rewards.add(reward);
        }
        return promotion;
    }

    private PromotionReward reward(RewardType type, String value, TargetScope scope, String targetCode) {
        return PromotionReward.builder()
                .rewardType(type)
                .value(value == null ? null : new BigDecimal(value))
                .targetScope(scope)
                .targetCode(targetCode)
                .build();
    }

    private void condition(Promotion promotion, ConditionType type, ConditionOperator operator,
                           String value, String valueList) {
        PromotionCondition condition = PromotionCondition.builder()
                .promotion(promotion)
                .conditionType(type)
                .operator(operator)
                .value(value)
                .valueList(valueList)
                .build();
        condition.setId(ids.getAndIncrement());
        conditions.add(condition);
    }

    private PricingContext.PricingLine line(String reference, TargetScope scope, String code,
                                            int quantity, String unitPrice) {
        return new PricingContext.PricingLine(reference, scope, code, null, code,
                quantity, new BigDecimal(unitPrice), null);
    }

    private PricingContext context(PricingContext.PricingLine... lines) {
        return context(false, lines);
    }

    private PricingContext context(boolean firstPurchase, PricingContext.PricingLine... lines) {
        return new PricingContext(
                "MEMBER", "MEM-1", null, null, null, firstPurchase,
                List.of(lines), null, null, null, null, "XAF", List.of(), Instant.now(), true);
    }
}
