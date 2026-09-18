package com.sni.bokaticowork.features.subscription.promotion.pricing.engine;

import com.sni.bokaticowork.features.subscription.promotion.enums.DiscountType;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.service.PromotionAudienceFilter;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.PromotionType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.RewardType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.TargetScope;
import com.sni.bokaticowork.features.subscription.promotion.pricing.model.PromotionReward;
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
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Le prix affiché dans une vitrine.
 *
 * <p>Une promotion visible seulement au moment de payer ne vend rien : ce qui déclenche l'achat est
 * le prix barré vu au moment de choisir. Toute liste de prix vue par un client doit donc passer par
 * ce calcul plutôt que d'afficher le tarif catalogue brut.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PriceCatalogueTest {

    @Mock private PromotionRepository promotionRepository;
    @Mock private PromotionConditionRepository conditionRepository;
    @Mock private PromotionRewardRepository rewardRepository;
    @Mock private CouponLookup couponLookup;
    @Mock private PriceListResolver priceListResolver;
    @Mock private PromotionAudienceFilter audienceFilter;

    private PricingEngine engine;

    private final List<Promotion> promotions = new ArrayList<>();
    private final List<PromotionReward> rewards = new ArrayList<>();

    @BeforeEach
    void setUp() {
        engine = new PricingEngine(promotionRepository, conditionRepository, rewardRepository,
                new PromotionConditionEvaluator(), couponLookup, priceListResolver, audienceFilter);
        when(promotionRepository.findEvaluable(anyString(), any(Instant.class))).thenReturn(promotions);
        when(conditionRepository.findAllByPromotionIds(any())).thenReturn(List.of());
        when(rewardRepository.findAllByPromotionIds(any())).thenReturn(rewards);
        when(couponLookup.resolve(any(), any(), any(), any())).thenReturn(List.of());
        when(audienceFilter.visibleTo(any(), any())).thenAnswer(call -> {
            java.util.Collection<Long> given = call.getArgument(0);
            return given == null ? java.util.Set.of() : new HashSet<>(given);
        });
        when(priceListResolver.resolve(any())).thenAnswer(call -> {
            PricingContext given = call.getArgument(0);
            return new PriceListResolver.Resolution(given == null ? List.of() : given.linesOrEmpty(), List.of());
        });
    }

    @Test
    void showsTheCataloguePriceWhenNothingApplies() {
        List<EffectivePrice> prices = engine.priceCatalogue(context(), List.of(ref("PLN-1", 1, "10000")));

        EffectivePrice price = prices.getFirst();
        assertEquals(new BigDecimal("10000"), price.listPrice());
        assertEquals(new BigDecimal("10000"), price.effectivePrice());
        assertEquals(EffectivePrice.PriceSource.CATALOGUE, price.priceSource());
        assertFalse(price.discounted());
    }

    @Test
    void showsTheStruckPriceAndTheSaving() {
        promotion("RENTREE", reward(RewardType.PERCENTAGE_OFF, "20", TargetScope.WHOLE_ORDER, null));

        EffectivePrice price = engine.priceCatalogue(context(), List.of(ref("PLN-1", 1, "10000"))).getFirst();

        assertEquals(new BigDecimal("10000"), price.listPrice());
        assertEquals(new BigDecimal("8000"), price.effectivePrice());
        assertEquals(new BigDecimal("2000"), price.savingsAmount());
        assertEquals(new BigDecimal("20"), price.savingsPercent());
        assertEquals(EffectivePrice.PriceSource.PROMOTION, price.priceSource());
        assertEquals("RENTREE", price.promotionCode());
    }

    /** Un « moins 19,7 % » n'accroche personne : l'économie s'annonce en entier. */
    @Test
    void roundsTheSavingPercentToAWholeNumber() {
        promotion("PETITE", reward(RewardType.FIXED_AMOUNT_OFF, "1970", TargetScope.WHOLE_ORDER, null));

        EffectivePrice price = engine.priceCatalogue(context(), List.of(ref("PLN-1", 1, "10000"))).getFirst();

        assertEquals(new BigDecimal("20"), price.savingsPercent());
    }

    /**
     * Un tarif négocié n'est pas une promotion. L'afficher comme une réduction exceptionnelle
     * revient à suggérer chaque mois au partenaire qu'il pourrait obtenir mieux.
     */
    @Test
    void namesAGridAsANegotiatedPriceRatherThanAPromotion() {
        when(priceListResolver.resolve(any())).thenReturn(new PriceListResolver.Resolution(
                List.of(new PricingContext.PricingLine("PLN-1", TargetScope.PLAN, "PLN-1", null, "Plan",
                        1, new BigDecimal("7000"), null)),
                List.of(new PriceListResolver.PriceOverride("PLN-1", "GRT-PARTENAIRE", "Grille partenaire",
                        new BigDecimal("10000"), new BigDecimal("7000"), 1))));

        EffectivePrice price = engine.priceCatalogue(context(), List.of(ref("PLN-1", 1, "10000"))).getFirst();

        assertEquals(EffectivePrice.PriceSource.PRICE_LIST, price.priceSource());
        assertEquals("GRT-PARTENAIRE", price.promotionCode());
        assertEquals(new BigDecimal("7000"), price.effectivePrice());
    }

    /**
     * Chaque objet est tarifé seul. Une promotion portant sur l'ensemble d'une commande n'a pas de
     * sens dans une vitrine où le client n'a encore rien choisi : lui annoncer une réduction qui
     * suppose un panier qu'il n'a pas serait mensonger.
     */
    @Test
    void pricesEachItemOnItsOwn() {
        promotion("SALLES", reward(RewardType.PERCENTAGE_OFF, "10", TargetScope.RESOURCE, "SALLE-A"));

        List<EffectivePrice> prices = engine.priceCatalogue(context(), List.of(
                new PriceableRef(TargetScope.RESOURCE, "SALLE-A", null, "Salle A", 1, new BigDecimal("10000"), null, null),
                new PriceableRef(TargetScope.RESOURCE, "SALLE-B", null, "Salle B", 1, new BigDecimal("10000"), null, null)));

        assertTrue(prices.get(0).discounted());
        assertFalse(prices.get(1).discounted());
    }

    @Test
    void multipliesTheListPriceByTheQuantityAsked() {
        EffectivePrice price = engine.priceCatalogue(context(), List.of(ref("PLN-1", 3, "10000"))).getFirst();

        assertEquals(new BigDecimal("30000"), price.listPrice());
    }

    @Test
    void returnsNothingForAnEmptyList() {
        assertTrue(engine.priceCatalogue(context(), List.of()).isEmpty());
        assertTrue(engine.priceCatalogue(context(), null).isEmpty());
    }

    // -------------------------------------------------------------------------------------

    private void promotion(String code, PromotionReward... promotionRewards) {
        Promotion promotion = Promotion.builder()
                .code(code)
                .name("Campagne " + code)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.ZERO)
                .promotionType(PromotionType.AUTOMATIC)
                .priority(100)
                .stackable(Boolean.TRUE)
                .exclusive(Boolean.FALSE)
                .consumedBudgetAmount(BigDecimal.ZERO)
                .totalDiscountGranted(BigDecimal.ZERO)
                .redemptionCount(0)
                .startsAt(Instant.now().minusSeconds(3600))
                .build();
        promotion.setId((long) promotions.size() + 1);
        promotions.add(promotion);
        for (PromotionReward reward : promotionRewards) {
            reward.setPromotion(promotion);
            rewards.add(reward);
        }
    }

    private PromotionReward reward(RewardType type, String value, TargetScope scope, String targetCode) {
        return PromotionReward.builder()
                .rewardType(type)
                .value(new BigDecimal(value))
                .targetScope(scope)
                .targetCode(targetCode)
                .build();
    }

    private PriceableRef ref(String code, int quantity, String listPrice) {
        return new PriceableRef(TargetScope.PLAN, code, null, "Plan " + code,
                quantity, new BigDecimal(listPrice), null, "MONTHLY");
    }

    private PricingContext context() {
        return new PricingContext("MEMBER", "MEM-1", null, null, null, false,
                List.of(), "MONTHLY", null, null, null, "XAF", List.of(), Instant.now(), true);
    }
}
