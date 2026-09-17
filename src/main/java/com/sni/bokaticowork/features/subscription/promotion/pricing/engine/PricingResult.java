package com.sni.bokaticowork.features.subscription.promotion.pricing.engine;

import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.DiscountSourceType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.RewardType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ce que le moteur rend.
 *
 * <p>Le detail compte autant que le total. Un client a qui l'on annonce un prix sans dire d'ou
 * vient la reduction ne fait pas confiance au prix, et un commercial qui ne sait pas quelle regle
 * s'est appliquee ne peut pas en discuter.</p>
 *
 * @param rejections coupons et promotions ecartes, avec un motif redige en francais · un refus muet
 *                   renvoie le client vers le support
 */
public record PricingResult(
        /** Total au tarif catalogue, avant qu'une grille ne s'applique. */
        BigDecimal catalogTotal,
        /** Total avant promotions · egal au catalogue lorsqu'aucune grille ne s'applique. */
        BigDecimal originalTotal,
        BigDecimal discountTotal,
        BigDecimal finalTotal,
        String currency,
        List<AppliedRule> appliedRules,
        List<Rejection> rejections,
        List<NonMonetaryGrant> grants,
        /**
         * Substitutions de prix operees par une grille. Volontairement separees des remises : un
         * tarif negocie n'est pas une faveur ponctuelle et ne doit pas s'afficher comme telle.
         */
        List<PriceOverride> priceOverrides
) {

    /**
     * @param order rang d'application · l'ordre change le montant final, il doit etre restituable
     */
    public record AppliedRule(
            int order,
            DiscountSourceType sourceType,
            String sourceCode,
            String sourceName,
            RewardType rewardType,
            String lineReference,
            BigDecimal originalAmount,
            BigDecimal discountAmount,
            String explanation
    ) {
    }

    public record PriceOverride(
            String lineReference,
            String priceListCode,
            String priceListName,
            BigDecimal catalogUnitPrice,
            BigDecimal effectiveUnitPrice,
            int quantity
    ) {
    }

    public record Rejection(
            DiscountSourceType sourceType,
            String sourceCode,
            String reason
    ) {
    }

    /**
     * Une recompense qui ne se soustrait pas d'un montant · periodes offertes, droits accordes,
     * credit de portefeuille. Le moteur la constate, un autre module la materialise.
     */
    public record NonMonetaryGrant(
            DiscountSourceType sourceType,
            String sourceCode,
            RewardType rewardType,
            String targetCode,
            BigDecimal value,
            String explanation
    ) {
    }
}
