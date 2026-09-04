package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.features.billing.service.support.BillingTaxRuleResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le courriel de confirmation reconstitue la ventilation fiscale depuis le seul montant TTC de la
 * reservation. Il le faisait avec sa propre regle : diviseur {@code 1 + 0,184 + 0,05}, taux de TVA
 * de 18,4 % introuvable ailleurs, et centime additionnel calcule sur la base HT.
 *
 * <p>La facture, elle, applique la TVA au HT puis le centime additionnel <b>a la TVA</b>. Sur une
 * reservation de 20 000, le courriel annoncait 810 de centime additionnel la ou la facture en
 * portait 151. Les deux totaux tombaient juste, seule la ventilation differait, ce qui a rendu
 * l'ecart invisible.
 */
class BookingEmailTaxBreakdownTest {

    private static final BigDecimal VAT = new BigDecimal("18");
    private static final BigDecimal CENT = new BigDecimal("5");

    private final BookingEmailNotifier notifier = new BookingEmailNotifier(
            null, null, null, null);

    private final BillingTaxRuleResolver.TaxProfile profile =
            new BillingTaxRuleResolver.TaxProfile(VAT, CENT);

    @Test
    void shouldSplitTheTotalTheSameWayTheInvoiceDoes() {
        BigDecimal total = new BigDecimal("20000");

        BigDecimal base = invoke("computeTaxIncludedBase", total, profile);
        BigDecimal vat = invoke("computeVat", base, profile);
        BigDecimal cent = invoke("percentage", vat, CENT);

        // Valeurs relevees sur une facture reelle portant le meme total.
        assertThat(round(base)).isEqualByComparingTo("16820.86");
        assertThat(round(vat)).isEqualByComparingTo("3027.75");
        assertThat(round(cent)).isEqualByComparingTo("151.39");
    }

    @Test
    void shouldKeepTheComponentsAddingUpToTheTotal() {
        BigDecimal total = new BigDecimal("108900");

        BigDecimal base = invoke("computeTaxIncludedBase", total, profile);
        BigDecimal vat = invoke("computeVat", base, profile);
        BigDecimal cent = invoke("percentage", vat, CENT);

        assertThat(round(base.add(vat).add(cent))).isEqualByComparingTo(round(total));
    }

    @Test
    void shouldChargeTheAdditionalCentOnTheVatNotOnTheBase() {
        BigDecimal base = new BigDecimal("100000");
        BigDecimal vat = invoke("computeVat", base, profile);
        BigDecimal cent = invoke("percentage", vat, CENT);

        assertThat(round(vat)).isEqualByComparingTo("18000.00");
        // 5 % de la TVA, soit 900 · et non 5 % de la base, qui donnerait 5 000.
        assertThat(round(cent)).isEqualByComparingTo("900.00");
    }

    @Test
    void shouldPublishTheRatesItActuallyApplied() {
        // Le gabarit affichait « 18.4 % » en dur, un taux qui n'existe nulle part ailleurs.
        assertThat(this.<String>invoke("formatRate", VAT)).isEqualTo("18 %");
        assertThat(this.<String>invoke("formatRate", CENT)).isEqualTo("5 %");
        assertThat(this.<String>invoke("formatRate", new BigDecimal("18.50"))).isEqualTo("18,5 %");
    }

    @SuppressWarnings("unchecked")
    private <T> T invoke(String method, Object... args) {
        return (T) ReflectionTestUtils.invokeMethod(notifier, method, args);
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
