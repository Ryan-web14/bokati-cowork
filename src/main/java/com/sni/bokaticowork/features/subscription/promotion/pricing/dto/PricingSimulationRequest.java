package com.sni.bokaticowork.features.subscription.promotion.pricing.dto;

import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.TargetScope;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Demande de simulation.
 *
 * <p>Le meme moteur que l'application reelle, sans effet de bord. Trois usages : l'affichage du
 * panier cote client, l'aide a la vente cote commercial, et le test d'une campagne avant son
 * lancement.</p>
 */
public record PricingSimulationRequest(
        String subscriberType,
        String subscriberCode,
        String subscriberSegment,
        Integer kycLevel,
        Integer tenureMonths,
        Boolean firstPurchase,
        @NotEmpty @Valid List<Line> lines,
        String billingCycle,
        String channel,
        String paymentMethod,
        String locationCode,
        /**
         * Facultative · deduite du catalogue de la premiere ligne tarifee, sinon de la devise par
         * defaut de l'etablissement. La renseigner prime sur toute deduction.
         */
        String currency,
        List<String> couponCodes,
        /** Date d'evaluation, pour tester une campagne a venir sans attendre son ouverture. */
        Instant evaluationDate,
        Boolean promotionsAllowed
) {

    public record Line(
            String reference,
            @NotNull TargetScope scope,
            String code,
            String categoryCode,
            String label,
            int quantity,
            @NotNull BigDecimal unitPrice,
            BigDecimal setupFee
    ) {
    }
}
