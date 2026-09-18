package com.sni.bokaticowork.features.subscription.promotion.pricing.engine;

import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.TargetScope;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Tout ce que le moteur a le droit de savoir pour tarifer.
 *
 * <p>Le moteur ne lit rien d'autre : pas de depot, pas d'appel a un autre module. Ce qui n'est pas
 * dans le contexte n'existe pas pour lui. C'est ce qui rend une simulation exactement egale a
 * l'application reelle, et ce qui permet de tarifer une liste de ressources aussi bien qu'un
 * panier.</p>
 *
 * @param lines ce qui est achete · une ligne est un objet tarifable, designe par une portee et un
 *              code, sans que le moteur sache de quel module il vient
 */
public record PricingContext(
        String subscriberType,
        String subscriberCode,
        String subscriberSegment,
        Integer kycLevel,
        Integer tenureMonths,
        Boolean firstPurchase,
        List<PricingLine> lines,
        String billingCycle,
        String channel,
        String paymentMethod,
        String locationCode,
        String currency,
        List<String> couponCodes,
        Instant evaluationDate,
        /**
         * Le prix est deja negocie, aucune promotion ne s'y ajoute. Porte par un abonnement derive :
         * un tarif consenti par contrat n'a pas vocation a recevoir une remise supplementaire.
         */
        boolean promotionsAllowed
) {

    public Instant evaluationDateOrNow() {
        return evaluationDate == null ? Instant.now() : evaluationDate;
    }

    public List<String> couponCodesOrEmpty() {
        return couponCodes == null ? List.of() : couponCodes;
    }

    public List<PricingLine> linesOrEmpty() {
        return lines == null ? List.of() : lines;
    }

    /**
     * Le meme contexte, avec des lignes retarifees. Sert a l'etape 2 : une fois la grille appliquee,
     * tout ce qui suit doit voir le prix negocie et non le prix catalogue.
     */
    public PricingContext withLines(List<PricingLine> replacements) {
        return new PricingContext(subscriberType, subscriberCode, subscriberSegment, kycLevel, tenureMonths,
                firstPurchase, replacements, billingCycle, channel, paymentMethod, locationCode, currency,
                couponCodes, evaluationDate, promotionsAllowed);
    }

    /**
     * Une ligne tarifable.
     *
     * @param reference identifiant de la ligne dans le document appelant, rendu tel quel dans le
     *                  resultat pour que l'appelant sache a quoi rattacher la remise
     */
    public record PricingLine(
            String reference,
            TargetScope scope,
            String code,
            String categoryCode,
            String label,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal setupFee
    ) {

        public BigDecimal lineAmount() {
            BigDecimal unit = unitPrice == null ? BigDecimal.ZERO : unitPrice;
            return unit.multiply(BigDecimal.valueOf(Math.max(quantity, 0)));
        }

        public BigDecimal setupFeeOrZero() {
            return setupFee == null ? BigDecimal.ZERO : setupFee;
        }
    }
}
