package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.features.billing.service.support.BillingTaxRuleResolver;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Le detail des taxes d'un montant de reservation TTC, tire des regles de taxe en base.
 *
 * <p>L'e-mail et le PDF de confirmation calculaient chacun le leur. Le PDF figeait une TVA a
 * 18,4 % et un centime additionnel pris sur la base HT ; les regles en vigueur disent TVA 18 %
 * et centime additionnel de 5 % sur la TVA. Le meme montant affichait deux ventilations : sur une
 * reservation de 20 000, 810 de centime additionnel la ou la facture en portait 151.</p>
 *
 * @param base              montant hors taxes
 * @param vat               TVA
 * @param additionalCent    centime additionnel, calcule sur la TVA
 * @param vatPct            taux de TVA a afficher (« 18 % »)
 * @param additionalCentPct taux du centime additionnel a afficher (« 5 % »)
 */
public record BookingTaxBreakdown(BigDecimal base, BigDecimal vat, BigDecimal additionalCent,
                                  String vatPct, String additionalCentPct) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public static BookingTaxBreakdown of(BigDecimal totalAmount, BillingTaxRuleResolver.TaxProfile profile) {
        BigDecimal vatRate = profile == null || profile.vatRate() == null ? BigDecimal.ZERO : profile.vatRate();
        BigDecimal centRate = profile == null || profile.additionalCentRate() == null ? BigDecimal.ZERO : profile.additionalCentRate();
        if (totalAmount == null) {
            return new BookingTaxBreakdown(null, null, null, formatRate(vatRate), formatRate(centRate));
        }
        // TTC = HT x (1 + tva x (1 + centime)) · le centime additionnel porte sur la TVA.
        BigDecimal divisor = BigDecimal.ONE.add(fraction(vatRate).multiply(BigDecimal.ONE.add(fraction(centRate))));
        BigDecimal base = totalAmount.divide(divisor, 8, RoundingMode.HALF_UP);
        BigDecimal vat = percentage(base, vatRate);
        return new BookingTaxBreakdown(base, vat, percentage(vat, centRate), formatRate(vatRate), formatRate(centRate));
    }

    private static BigDecimal fraction(BigDecimal ratePercent) {
        return ratePercent.divide(HUNDRED, 8, RoundingMode.HALF_UP);
    }

    private static BigDecimal percentage(BigDecimal amount, BigDecimal ratePercent) {
        return amount.multiply(ratePercent).divide(HUNDRED, 8, RoundingMode.HALF_UP);
    }

    /** {@code 18} devient {@code 18 %}, {@code 18.50} devient {@code 18,5 %}. */
    static String formatRate(BigDecimal ratePercent) {
        return ratePercent.stripTrailingZeros().toPlainString().replace('.', ',') + " %";
    }
}
