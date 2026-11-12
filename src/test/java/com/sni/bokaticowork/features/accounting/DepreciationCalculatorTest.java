package com.sni.bokaticowork.features.accounting;

import com.sni.bokaticowork.features.accounting.service.support.DepreciationCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Calculs du lot F18 : lineaire au prorata, sans derive d'arrondi, degressif, plus ou moins-value. */
class DepreciationCalculatorTest {

    @Test
    @DisplayName("Ordinateur de 900 000 mis en service le 1er avril sur 36 mois : 36 dotations de 25 000")
    void linearFromFirstOfMonth() {
        List<DepreciationCalculator.Line> lines = DepreciationCalculator.linear(new BigDecimal("900000"),
                LocalDate.of(2026, 4, 1), LocalDate.of(2029, 4, 1), true);

        assertThat(lines).hasSize(36);
        assertThat(lines).allMatch(line -> line.amount().compareTo(new BigDecimal("25000")) == 0);
        assertThat(lines.get(0).period()).isEqualTo("2026-04");
        assertThat(lines.get(35).period()).isEqualTo("2029-03");
    }

    @Test
    @DisplayName("Mise en service en cours de mois : prorata au jour, la somme egale toujours la base")
    void linearProrataSumsToBase() {
        BigDecimal base = new BigDecimal("1000003");
        List<DepreciationCalculator.Line> lines = DepreciationCalculator.linear(base, LocalDate.of(2026, 4, 16), LocalDate.of(2029, 4, 16), true);

        BigDecimal total = lines.stream().map(DepreciationCalculator.Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo(base);
        assertThat(lines.get(0).amount()).isLessThan(lines.get(1).amount());
    }

    @Test
    @DisplayName("Degressif : premiere annuite au taux degressif, somme egale a la base")
    void decliningSumsToBase() {
        BigDecimal rate = DepreciationCalculator.decliningRate(60, new BigDecimal("2"));
        List<DepreciationCalculator.Line> lines = DepreciationCalculator.declining(new BigDecimal("1000000"), YearMonth.of(2026, 1), 60, rate);

        BigDecimal total = lines.stream().map(DepreciationCalculator.Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("1000000");
        BigDecimal firstYear = lines.subList(0, 12).stream().map(DepreciationCalculator.Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(firstYear).isEqualByComparingTo("400000");
    }

    @Test
    @DisplayName("Coefficients du degressif selon la duree")
    void coefficients() {
        String table = "3-4:1.5,5-6:2,7+:2.5";
        assertThat(DepreciationCalculator.coefficient(36, table)).isEqualByComparingTo("1.5");
        assertThat(DepreciationCalculator.coefficient(60, table)).isEqualByComparingTo("2");
        assertThat(DepreciationCalculator.coefficient(120, table)).isEqualByComparingTo("2.5");
    }

    @Test
    @DisplayName("Cession sous la valeur nette : moins-value")
    void gainOrLoss() {
        assertThat(DepreciationCalculator.gainOrLoss(new BigDecimal("825000"), new BigDecimal("850000"))).isEqualByComparingTo("-25000");
        assertThat(DepreciationCalculator.gainOrLoss(new BigDecimal("900000"), new BigDecimal("850000"))).isEqualByComparingTo("50000");
    }
}
