package com.sni.bokaticowork.features.subscription.promotion.pricing.dto;

import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ce que la simulation rend au client.
 *
 * <p>Le detail regle par regle compte autant que le total : un prix annonce sans dire d'ou vient la
 * reduction n'inspire pas confiance, et un refus muet renvoie le client vers le support.</p>
 */
public record PricingSimulationResponse(
        /** Total au tarif catalogue. Egal au suivant lorsqu'aucune grille ne s'applique. */
        BigDecimal catalogTotal,
        BigDecimal originalTotal,
        BigDecimal discountTotal,
        BigDecimal finalTotal,
        String currency,
        List<PricingResult.AppliedRule> appliedRules,
        List<PricingResult.Rejection> rejections,
        List<PricingResult.NonMonetaryGrant> grants,
        /**
         * Substitutions de prix operees par une grille, separees des remises a dessein · un tarif
         * negocie ne doit pas s'afficher au client comme une reduction exceptionnelle.
         */
        List<PricingResult.PriceOverride> priceOverrides
) {

    public static PricingSimulationResponse from(PricingResult result) {
        return new PricingSimulationResponse(
                result.catalogTotal(),
                result.originalTotal(),
                result.discountTotal(),
                result.finalTotal(),
                result.currency(),
                result.appliedRules(),
                result.rejections(),
                result.grants(),
                result.priceOverrides());
    }
}
