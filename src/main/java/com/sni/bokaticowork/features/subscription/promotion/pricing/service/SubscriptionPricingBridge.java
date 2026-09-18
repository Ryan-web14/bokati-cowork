package com.sni.bokaticowork.features.subscription.promotion.pricing.service;

import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingContext;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingEngine;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingResult;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.DiscountDocumentType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.RewardType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.TargetScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Fait entrer le moteur de tarification dans la souscription d'un abonnement ou d'un pass.
 *
 * <p>L'ordre importe et il est le meme que pour une facture : <b>la remise s'applique avant la
 * taxe</b>. La taxe porte sur ce que le client paie reellement, pas sur un tarif catalogue qu'il ne
 * paie jamais. Inverser reviendrait a lui facturer de la TVA sur une somme qui ne lui est pas
 * reclamee.</p>
 *
 * <p>Le montant recurrent et les frais d'entree sont reduits separement, parce qu'ils ne finissent
 * pas au meme endroit : le premier devient le prix de l'abonnement, les seconds une ligne de
 * facturation initiale. Confondre les deux ferait disparaitre une dispense de frais dans le prix
 * mensuel, ou l'inverse.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionPricingBridge {

    private final PricingEngine pricingEngine;
    private final DiscountApplicationService discountApplicationService;

    /**
     * @param recurringAmount montant recurrent apres grille et remises, avant taxe
     * @param setupFee        frais d'entree apres dispense eventuelle, avant taxe
     */
    public record PricedSubscription(
            BigDecimal recurringAmount,
            BigDecimal setupFee,
            BigDecimal catalogRecurringAmount,
            BigDecimal catalogSetupFee,
            PricingResult result
    ) {

        public boolean unchanged() {
            return result == null
                    || (result.appliedRules().isEmpty() && result.priceOverrides().isEmpty());
        }
    }

    /**
     * Tarife une souscription sans rien persister ni consommer.
     *
     * <p>Appele avant la creation, il peut l'etre plusieurs fois · une simulation commerciale, un
     * devis, puis la souscription reelle. Aucun budget n'est touche tant que {@link #confirm} n'a
     * pas ete appele.</p>
     */
    @Transactional(readOnly = true)
    public PricedSubscription price(String subscriberType,
                                    String subscriberCode,
                                    String segment,
                                    TargetScope scope,
                                    String planCode,
                                    String billingCycle,
                                    String currency,
                                    BigDecimal recurringAmount,
                                    BigDecimal setupFee,
                                    List<String> couponCodes,
                                    boolean promotionsAllowed) {
        BigDecimal catalogRecurring = nonNull(recurringAmount);
        BigDecimal catalogSetup = nonNull(setupFee);
        if (catalogRecurring.signum() <= 0 && catalogSetup.signum() <= 0) {
            return new PricedSubscription(catalogRecurring, catalogSetup, catalogRecurring, catalogSetup, null);
        }

        PricingContext context = toContext(subscriberType, subscriberCode, segment, scope, planCode,
                billingCycle, currency, catalogRecurring, catalogSetup, couponCodes, promotionsAllowed);
        PricingResult result = pricingEngine.evaluate(context);

        // La grille remplace le prix catalogue · elle arrive donc dans le montant, jamais en remise.
        BigDecimal effectiveRecurring = result.priceOverrides().stream()
                .filter(override -> planCode.equals(override.lineReference()))
                .map(PricingResult.PriceOverride::effectiveUnitPrice)
                .findFirst()
                .orElse(catalogRecurring);

        BigDecimal waivedSetup = sumOf(result, true);
        BigDecimal otherDiscount = sumOf(result, false);

        return new PricedSubscription(
                floorAtZero(effectiveRecurring.subtract(otherDiscount)),
                floorAtZero(catalogSetup.subtract(waivedSetup)),
                catalogRecurring,
                catalogSetup,
                result);
    }

    /** Trace les remises accordees et consomme les budgets, une fois la souscription creee. */
    @Transactional
    public void confirm(PricedSubscription priced,
                        String subscriberType,
                        String subscriberCode,
                        String segment,
                        TargetScope scope,
                        String planCode,
                        String billingCycle,
                        String currency,
                        List<String> couponCodes,
                        DiscountDocumentType documentType,
                        String documentCode,
                        String appliedBy) {
        if (priced == null || priced.result() == null || priced.result().appliedRules().isEmpty()) {
            return;
        }
        PricingContext context = toContext(subscriberType, subscriberCode, segment, scope, planCode,
                billingCycle, currency, priced.catalogRecurringAmount(), priced.catalogSetupFee(),
                couponCodes, true);
        discountApplicationService.apply(context, documentType, documentCode, appliedBy);
    }

    // -----------------------------------------------------------------------------------------

    private PricingContext toContext(String subscriberType,
                                     String subscriberCode,
                                     String segment,
                                     TargetScope scope,
                                     String planCode,
                                     String billingCycle,
                                     String currency,
                                     BigDecimal recurringAmount,
                                     BigDecimal setupFee,
                                     List<String> couponCodes,
                                     boolean promotionsAllowed) {
        // Une seule ligne, dont la reference est le code du plan : c'est elle que la grille
        // retarifera, et c'est par cette reference qu'on retrouvera son prix negocie.
        PricingContext.PricingLine line = new PricingContext.PricingLine(
                planCode, scope, planCode, null, planCode, 1, recurringAmount, setupFee);

        return new PricingContext(
                subscriberType, subscriberCode, segment, null, null, null,
                List.of(line), billingCycle, "SUBSCRIPTION", null, null, currency,
                couponCodes == null ? List.of() : couponCodes,
                Instant.now(), promotionsAllowed);
    }

    /**
     * Somme des remises, selon qu'elles portent ou non sur les frais d'entree.
     *
     * <p>La separation n'est possible que parce que le moteur rend une regle par recompense. Une
     * promotion accordant a la fois un pourcentage et la dispense de frais produit deux lignes, et
     * chacune retrouve sa destination.</p>
     */
    private BigDecimal sumOf(PricingResult result, boolean setupFeeWaiver) {
        return result.appliedRules().stream()
                .filter(rule -> (rule.rewardType() == RewardType.WAIVE_SETUP_FEE) == setupFeeWaiver)
                .map(PricingResult.AppliedRule::discountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal floorAtZero(BigDecimal value) {
        return value.signum() < 0 ? BigDecimal.ZERO : value;
    }

    private BigDecimal nonNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
