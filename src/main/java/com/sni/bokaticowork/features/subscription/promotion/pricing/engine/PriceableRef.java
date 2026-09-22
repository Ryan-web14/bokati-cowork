package com.sni.bokaticowork.features.subscription.promotion.pricing.engine;

import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.TargetScope;

import java.math.BigDecimal;

/**
 * Un objet dont on veut connaitre le prix affiche.
 *
 * <p>Distinct d'une ligne de panier : on ne l'achete pas, on le regarde. Le moteur doit savoir
 * tarifer une liste et pas seulement un panier, parce qu'une promotion visible seulement au moment
 * de payer ne vend rien · ce qui declenche l'achat est le prix barre vu au moment de choisir.</p>
 */
public record PriceableRef(
        TargetScope scope,
        String code,
        String categoryCode,
        String label,
        /** Facultative · un catalogue qu'on regarde parle d'unites, pas de quantites. Un par defaut. */
        Integer quantity,
        BigDecimal listPrice,
        BigDecimal setupFee,
        String billingCycle
) {

    public int quantityOrOne() {
        return quantity == null || quantity <= 0 ? 1 : quantity;
    }

    public BigDecimal listPriceOrZero() {
        return listPrice == null ? BigDecimal.ZERO : listPrice;
    }
}
