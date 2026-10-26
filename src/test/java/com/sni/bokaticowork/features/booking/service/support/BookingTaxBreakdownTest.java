package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.features.billing.service.support.BillingTaxRuleResolver;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le courriel et le PDF de confirmation reconstituent la ventilation fiscale depuis le seul montant
 * TTC de la reservation. Chacun le faisait avec sa propre regle : diviseur {@code 1 + 0,184 + 0,05},
 * taux de TVA de 18,4 % introuvable ailleurs, et centime additionnel calcule sur la base HT.
 *
 * <p>La facture, elle, applique la TVA au HT puis le centime additionnel <b>a la TVA</b>. Sur une
 * reservation de 20 000, le courriel annoncait 810 de centime additionnel la ou la facture en
 * portait 151. Les deux totaux tombaient juste, seule la ventilation differait, ce qui a rendu
 * l'ecart invisible.
 */
class BookingTaxBreakdownTest {

    private static final BigDecimal VAT = new BigDecimal("18");
    private static final BigDecimal CENT = new BigDecimal("5");

    private final BillingTaxRuleResolver.TaxProfile profile =
            new BillingTaxRuleResolver.TaxProfile(VAT, CENT);

    @Test
    void shouldSplitTheTotalTheSameWayTheInvoiceDoes() {
        BookingTaxBreakdown taxes = BookingTaxBreakdown.of(new BigDecimal("20000"), profile);

        // Valeurs relevees sur une facture reelle portant le meme total.
        assertThat(round(taxes.base())).isEqualByComparingTo("16820.86");
        assertThat(round(taxes.vat())).isEqualByComparingTo("3027.75");
        assertThat(round(taxes.additionalCent())).isEqualByComparingTo("151.39");
    }

    @Test
    void shouldKeepTheComponentsAddingUpToTheTotal() {
        BigDecimal total = new BigDecimal("108900");
        BookingTaxBreakdown taxes = BookingTaxBreakdown.of(total, profile);

        assertThat(round(taxes.base().add(taxes.vat()).add(taxes.additionalCent())))
                .isEqualByComparingTo(round(total));
    }

    @Test
    void shouldChargeTheAdditionalCentOnTheVatNotOnTheBase() {
        // 100 000 HT donnent 118 900 TTC : TVA 18 000, centime 900 (5 % de la TVA, et non 5 000).
        BookingTaxBreakdown taxes = BookingTaxBreakdown.of(new BigDecimal("118900"), profile);

        assertThat(round(taxes.vat())).isEqualByComparingTo("18000.00");
        assertThat(round(taxes.additionalCent())).isEqualByComparingTo("900.00");
    }

    @Test
    void shouldPublishTheRatesItActuallyApplied() {
        // Les gabarits affichaient « 18.4 % » en dur, un taux qui n'existe nulle part ailleurs.
        BookingTaxBreakdown taxes = BookingTaxBreakdown.of(new BigDecimal("1000"), profile);
        assertThat(taxes.vatPct()).isEqualTo("18 %");
        assertThat(taxes.additionalCentPct()).isEqualTo("5 %");
        assertThat(BookingTaxBreakdown.formatRate(new BigDecimal("18.50"))).isEqualTo("18,5 %");
    }

    @Test
    void shouldLeaveAmountsEmptyWithoutATotal() {
        BookingTaxBreakdown taxes = BookingTaxBreakdown.of(null, profile);
        assertThat(taxes.base()).isNull();
        assertThat(taxes.vatPct()).isEqualTo("18 %");
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
