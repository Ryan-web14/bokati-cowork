package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.service.support.BillingTaxRuleResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ventile un prix de plan entre base taxable, taxe et total.
 *
 * <p>Le meme algorithme etait ecrit deux fois, dans l'operateur de creation d'abonnement et dans
 * celui de creation de pass, avec les memes fonctions utilitaires recopiees. Deux copies d'un calcul
 * de taxe ne restent identiques que tant que personne ne touche a l'une des deux, et la divergence
 * ne se serait vue que sur une facture.</p>
 *
 * <p>Deux conventions y sont figees, et elles comptent autant que la formule.</p>
 *
 * <p><b>Le centime additionnel se calcule sur la TVA, pas sur la base.</b> C'est une taxe sur la
 * taxe, d'ou le facteur {@code 1 + tva + tva * cent} lorsque le prix est annonce taxe comprise.</p>
 *
 * <p><b>La caution n'est pas taxee.</b> Elle s'ajoute au total sans passer par la base taxable :
 * ce n'est pas un produit, c'est une somme detenue pour le compte du client et restituable. La
 * taxer reviendrait a taxer de l'argent qui ne nous appartient pas.</p>
 */
@Component
@RequiredArgsConstructor
public class PlanPriceAmountCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int MONEY_SCALE = 4;

    private final BillingTaxRuleResolver taxRuleResolver;

    /**
     * @param recurringAmount prix du plan pour la periode, ou prix du pass
     * @param setupFee        frais d'entree, taxes au meme titre que le prix
     * @param depositAmount   caution, ajoutee au total sans etre taxee
     * @param taxIncluded     le prix annonce contient-il deja la taxe
     */
    public Amounts compute(BigDecimal recurringAmount,
                           BigDecimal setupFee,
                           BigDecimal depositAmount,
                           Boolean taxIncluded) {
        BigDecimal base = money(nonNegative(recurringAmount));
        BigDecimal setup = money(nonNegative(setupFee));
        BigDecimal deposit = money(nonNegative(depositAmount));
        BigDecimal taxable = money(base.add(setup));
        BillingTaxRuleResolver.TaxProfile profile = taxRuleResolver.defaultTaxProfile();

        if (Boolean.TRUE.equals(taxIncluded)) {
            BigDecimal factor = taxFactor(profile.vatRate(), profile.additionalCentRate());
            BigDecimal subtotal = money(taxable.divide(factor, 8, RoundingMode.HALF_UP));
            return new Amounts(subtotal, money(taxable.subtract(subtotal)), money(taxable.add(deposit)));
        }

        BigDecimal vat = percentage(taxable, profile.vatRate());
        BigDecimal additionalCent = percentage(vat, profile.additionalCentRate());
        BigDecimal tax = money(vat.add(additionalCent));
        return new Amounts(taxable, tax, money(taxable.add(tax).add(deposit)));
    }

    public record Amounts(BigDecimal subtotal, BigDecimal tax, BigDecimal total) {
    }

    private BigDecimal taxFactor(BigDecimal vatRate, BigDecimal additionalCentRate) {
        BigDecimal vat = rateFactor(vatRate);
        return BigDecimal.ONE.add(vat).add(vat.multiply(rateFactor(additionalCentRate)));
    }

    private BigDecimal rateFactor(BigDecimal rate) {
        if (rate == null || rate.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return rate.divide(HUNDRED, 8, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal rate) {
        if (rate == null || rate.signum() == 0) {
            return BigDecimal.ZERO;
        }
        if (rate.signum() < 0) {
            throw new BadRequestException("Percentage rate cannot be negative");
        }
        return money(amount.multiply(rate).divide(HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP));
    }

    private BigDecimal nonNegative(BigDecimal value) {
        BigDecimal resolved = value == null ? BigDecimal.ZERO : value;
        if (resolved.signum() < 0) {
            throw new BadRequestException("Plan price amounts cannot be negative");
        }
        return resolved;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
