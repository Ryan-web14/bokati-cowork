package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CashChangeTest {

    private static BigDecimal xaf(String value) {
        return new BigDecimal(value);
    }

    @Test
    @DisplayName("Le client tend plus que le du : on encaisse le du et on rend la difference")
    void tenderedAboveDueGivesChange() {
        CashChange cash = CashChange.compute(xaf("17500"), null, xaf("20000"));

        assertThat(cash.appliedAmount()).isEqualByComparingTo("17500");
        assertThat(cash.tenderedAmount()).isEqualByComparingTo("20000");
        assertThat(cash.changeAmount()).isEqualByComparingTo("2500");
    }

    @Test
    @DisplayName("Le client tend le compte juste : rien a rendre")
    void exactAmountGivesNoChange() {
        CashChange cash = CashChange.compute(xaf("17500"), null, xaf("17500"));

        assertThat(cash.appliedAmount()).isEqualByComparingTo("17500");
        assertThat(cash.changeAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Le client tend moins que le du : paiement partiel du montant tendu, rien a rendre")
    void tenderedBelowDueIsAPartialPayment() {
        CashChange cash = CashChange.compute(xaf("17500"), null, xaf("10000"));

        assertThat(cash.appliedAmount()).isEqualByComparingTo("10000");
        assertThat(cash.changeAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Montant a encaisser explicite : la monnaie se calcule sur lui")
    void explicitAmountDrivesTheChange() {
        CashChange cash = CashChange.compute(xaf("17500"), xaf("5000"), xaf("10000"));

        assertThat(cash.appliedAmount()).isEqualByComparingTo("5000");
        assertThat(cash.changeAmount()).isEqualByComparingTo("5000");
    }

    @Test
    @DisplayName("Montant remis inferieur au montant explicite : refuse")
    void tenderedBelowExplicitAmountIsRefused() {
        assertThatThrownBy(() -> CashChange.compute(xaf("17500"), xaf("17500"), xaf("10000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("inférieur");
    }

    @Test
    @DisplayName("Sans montant remis : comportement d'avant, le reste a payer, sans monnaie")
    void withoutTenderedAmountNothingChanges() {
        CashChange cash = CashChange.compute(xaf("17500"), null, null);

        assertThat(cash.appliedAmount()).isEqualByComparingTo("17500");
        assertThat(cash.tenderedAmount()).isNull();
        assertThat(cash.changeAmount()).isNull();
    }

    @Test
    @DisplayName("Montant remis nul ou negatif : refuse")
    void nonPositiveTenderedIsRefused() {
        assertThatThrownBy(() -> CashChange.compute(xaf("17500"), null, xaf("0")))
                .isInstanceOf(BadRequestException.class);
    }
}
