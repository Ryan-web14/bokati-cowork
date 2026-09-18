package com.sni.bokaticowork.features.subscription.promotion.pricing.engine;

import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.TargetScope;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Le prix a afficher, et d'ou il vient.
 *
 * <p>{@code priceSource} est le champ qui permet d'afficher honnetement. Un tarif negocie n'est pas
 * une promotion : l'afficher comme une reduction exceptionnelle revient a suggerer chaque mois au
 * partenaire qu'il pourrait obtenir mieux.</p>
 *
 * @param requiresCoupon      vrai si le prix suppose la saisie d'un code que le client n'a pas encore
 * @param conditionsSummary   ce qu'il reste a remplir, en une phrase lisible
 * @param validUntil          echeance de l'offre · elle cree l'urgence, et elle engage
 */
public record EffectivePrice(
        TargetScope scope,
        String code,
        String label,
        BigDecimal listPrice,
        BigDecimal effectivePrice,
        BigDecimal savingsAmount,
        BigDecimal savingsPercent,
        PriceSource priceSource,
        String promotionCode,
        String promotionLabel,
        Instant validUntil,
        boolean requiresCoupon,
        String conditionsSummary,
        String currency
) {

    public enum PriceSource {

        /** Le tarif du catalogue, sans rien appliquer. */
        CATALOGUE,

        /** Un tarif negocie, qui remplace le catalogue et ne s'affiche pas comme une remise. */
        PRICE_LIST,

        /** Une campagne, qui se barre et s'annonce. */
        PROMOTION,

        /** Un prix impose a la main, hors campagne. */
        OVERRIDE
    }

    public boolean discounted() {
        return savingsAmount != null && savingsAmount.signum() > 0;
    }
}
